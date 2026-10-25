package com.smartcollege.demo.controller;

import com.smartcollege.demo.dto.*;
import com.smartcollege.demo.entity.BookingStatus;
import com.smartcollege.demo.entity.Role;
import com.smartcollege.demo.exception.UnauthorizedException;
import com.smartcollege.demo.security.UserPrincipal;
import com.smartcollege.demo.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        BookingDto created = bookingService.createBooking(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking request created successfully", created));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        List<BookingDto> bookings = bookingService.getMyBookings(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("User bookings retrieved successfully", bookings));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<PageResponse<BookingDto>>> getPendingBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<BookingDto> pending = bookingService.getPendingBookings(pageable);
        return ResponseEntity.ok(ApiResponse.success("Pending booking requests retrieved successfully", pending));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDto>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = principal.getRole() == Role.ADMIN;
        BookingDto booking = bookingService.getBookingById(id, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Booking details retrieved successfully", booking));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<ResourceAvailabilityDto>> checkAvailability(
            @RequestParam Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        ResourceAvailabilityDto availability = bookingService.checkAvailability(resourceId, date, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success("Availability details retrieved successfully", availability));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = principal.getRole() == Role.ADMIN;
        BookingDto cancelled = bookingService.cancelBooking(id, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", cancelled));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<BookingDto>> reviewBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        BookingDto reviewed = bookingService.reviewBooking(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking reviewed successfully", reviewed));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<BookingDto>> approveBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingApproveRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        BookingDto approved = bookingService.approveBooking(id, request, principal.getId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.success("Booking request approved successfully", approved));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<BookingDto>> rejectBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingRejectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        BookingDto rejected = bookingService.rejectBooking(id, request, principal.getId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.success("Booking request rejected successfully", rejected));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<PageResponse<BookingDto>>> getAllBookings(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<BookingDto> bookings = bookingService.getAllBookings(resourceId, userId, status, date, pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings list retrieved successfully", bookings));
    }
}

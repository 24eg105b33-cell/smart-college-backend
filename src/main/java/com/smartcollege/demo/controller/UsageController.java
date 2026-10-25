package com.smartcollege.demo.controller;

import com.smartcollege.demo.dto.*;
import com.smartcollege.demo.entity.Role;
import com.smartcollege.demo.entity.UsageStatus;
import com.smartcollege.demo.exception.UnauthorizedException;
import com.smartcollege.demo.security.UserPrincipal;
import com.smartcollege.demo.service.UsageRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageRecordService usageRecordService;

    public UsageController(UsageRecordService usageRecordService) {
        this.usageRecordService = usageRecordService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<UsageRecordDto>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = principal.getRole() == Role.ADMIN;
        UsageRecordDto record = usageRecordService.checkIn(request, principal.getId(), isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource checked in successfully", record));
    }

    @RequestMapping(value = "/{id}/check-out", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<UsageRecordDto>> checkOut(
            @PathVariable Long id,
            @RequestBody(required = false) CheckOutRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = principal.getRole() == Role.ADMIN;
        UsageRecordDto record = usageRecordService.checkOut(id, request, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Resource checked out successfully", record));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<UsageRecordDto>>> getMyUsage(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        List<UsageRecordDto> records = usageRecordService.getMyUsageRecords(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("User usage history retrieved successfully", records));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsageRecordDto>> getUsageById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isPrivileged = principal.getRole() == Role.ADMIN || principal.getRole() == Role.FACULTY;
        UsageRecordDto record = usageRecordService.getUsageRecordById(id, principal.getId(), isPrivileged);
        return ResponseEntity.ok(ApiResponse.success("Usage record retrieved successfully", record));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<PageResponse<UsageRecordDto>>> getAllUsage(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) UsageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "checkInTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<UsageRecordDto> response = usageRecordService.getAllUsageRecords(resourceId, userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Usage records retrieved successfully", response));
    }
}

package com.smartcollege.demo.service;

import com.smartcollege.demo.dto.*;
import com.smartcollege.demo.entity.Booking;
import com.smartcollege.demo.entity.BookingStatus;
import com.smartcollege.demo.entity.Role;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingService {

    BookingDto createBooking(BookingRequest request, Long userId);

    List<BookingDto> getMyBookings(Long userId);

    BookingDto getBookingById(Long id, Long currentUserId, boolean isAdmin);

    ResourceAvailabilityDto checkAvailability(Long resourceId, LocalDate date, LocalTime startTime, LocalTime endTime);

    BookingDto cancelBooking(Long id, Long currentUserId, boolean isAdmin);

    BookingDto reviewBooking(Long id, BookingReviewRequest request, Long adminId);

    PageResponse<BookingDto> getPendingBookings(Pageable pageable);

    BookingDto approveBooking(Long id, BookingApproveRequest request, Long reviewerId, Role reviewerRole);

    BookingDto rejectBooking(Long id, BookingRejectRequest request, Long reviewerId, Role reviewerRole);

    PageResponse<BookingDto> getAllBookings(Long resourceId, Long userId, BookingStatus status, LocalDate date, Pageable pageable);

    Booking getEntityById(Long id);
}

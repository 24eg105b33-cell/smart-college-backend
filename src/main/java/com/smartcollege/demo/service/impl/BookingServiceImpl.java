package com.smartcollege.demo.service.impl;

import com.smartcollege.demo.dto.*;
import com.smartcollege.demo.entity.*;
import com.smartcollege.demo.exception.BadRequestException;
import com.smartcollege.demo.exception.ConflictException;
import com.smartcollege.demo.exception.ResourceNotFoundException;
import com.smartcollege.demo.repository.BookingRepository;
import com.smartcollege.demo.repository.ResourceRepository;
import com.smartcollege.demo.repository.UserRepository;
import com.smartcollege.demo.service.BookingService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private boolean facultyApprovalEnabled;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository,
                              @org.springframework.beans.factory.annotation.Value("${app.booking.faculty-approval-enabled:false}") boolean facultyApprovalEnabled) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.facultyApprovalEnabled = facultyApprovalEnabled;
    }

    public boolean isFacultyApprovalEnabled() {
        return facultyApprovalEnabled;
    }

    public void setFacultyApprovalEnabled(boolean facultyApprovalEnabled) {
        this.facultyApprovalEnabled = facultyApprovalEnabled;
    }

    @Override
    public BookingDto createBooking(BookingRequest request, Long userId) {
        validateBookingDateTime(request.getBookingDate(), request.getStartTime(), request.getEndTime());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.isActive()) {
            throw new BadRequestException("User account is inactive");
        }

        // Lock resource row to prevent concurrent race conditions
        Resource resource = resourceRepository.findByIdWithLock(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        if (!resource.isActive() || resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new BadRequestException("Resource is currently " + resource.getStatus() + " and cannot be booked");
        }

        // Prevent overlapping APPROVED bookings
        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                resource.getId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime(),
                BookingStatus.APPROVED,
                null
        );

        if (!conflicts.isEmpty()) {
            throw new ConflictException("The requested resource is already booked for this time slot");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setResource(resource);
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose().trim());
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);
        return BookingDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(userId).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long id, Long currentUserId, boolean isAdmin) {
        Booking booking = getEntityById(id);

        if (!isAdmin && !booking.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to view this booking");
        }

        return BookingDto.fromEntity(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceAvailabilityDto checkAvailability(Long resourceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + resourceId));

        List<Booking> approvedBookings = bookingRepository
                .findByResourceIdAndBookingDateAndStatusOrderByStartTimeAsc(resourceId, date, BookingStatus.APPROVED);

        List<TimeSlotDto> bookedSlots = approvedBookings.stream()
                .map(b -> new TimeSlotDto(b.getId(), b.getStartTime(), b.getEndTime(), b.getStatus(), b.getPurpose()))
                .collect(Collectors.toList());

        if (!resource.isActive() || resource.getStatus() != ResourceStatus.AVAILABLE) {
            return new ResourceAvailabilityDto(
                    resource.getId(),
                    resource.getName(),
                    resource.getResourceCode(),
                    date,
                    false,
                    "Resource is " + resource.getStatus(),
                    bookedSlots
            );
        }

        boolean isSlotAvailable = true;
        String message = "Resource is available";

        if (startTime != null && endTime != null) {
            if (!startTime.isBefore(endTime)) {
                throw new BadRequestException("End time must be after start time");
            }

            boolean hasOverlap = approvedBookings.stream()
                    .anyMatch(b -> b.getStartTime().isBefore(endTime) && b.getEndTime().isAfter(startTime));

            if (hasOverlap) {
                isSlotAvailable = false;
                message = "Resource has an overlapping booking during this time slot";
            }
        }

        return new ResourceAvailabilityDto(
                resource.getId(),
                resource.getName(),
                resource.getResourceCode(),
                date,
                isSlotAvailable,
                message,
                bookedSlots
        );
    }

    @Override
    public BookingDto cancelBooking(Long id, Long currentUserId, boolean isAdmin) {
        Booking booking = getEntityById(id);

        if (!isAdmin && !booking.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.REJECTED) {
            throw new BadRequestException("Cannot cancel a rejected booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        return BookingDto.fromEntity(saved);
    }

    @Override
    public BookingDto reviewBooking(Long id, BookingReviewRequest request, Long adminId) {
        Booking booking = getEntityById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot review a cancelled booking");
        }

        if (request.getStatus() == BookingStatus.APPROVED) {
            // Re-check for any overlapping approved bookings under lock
            resourceRepository.findByIdWithLock(booking.getResource().getId());

            List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                    booking.getResource().getId(),
                    booking.getBookingDate(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    BookingStatus.APPROVED,
                    booking.getId()
            );

            if (!conflicts.isEmpty()) {
                throw new ConflictException("Cannot approve: resource is already booked for this time slot");
            }

            booking.setStatus(BookingStatus.APPROVED);
        } else if (request.getStatus() == BookingStatus.REJECTED) {
            booking.setStatus(BookingStatus.REJECTED);
        } else {
            throw new BadRequestException("Invalid review status. Must be APPROVED or REJECTED");
        }

        if (request.getAdminRemarks() != null) {
            booking.setAdminRemarks(request.getAdminRemarks().trim());
        }

        if (adminId != null) {
            userRepository.findById(adminId).ifPresent(reviewer -> {
                booking.setReviewedBy(reviewer);
                booking.setReviewedAt(java.time.LocalDateTime.now());
            });
        }

        Booking saved = bookingRepository.save(booking);
        return BookingDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingDto> getPendingBookings(Pageable pageable) {
        Page<Booking> page = bookingRepository.findByStatus(BookingStatus.PENDING, pageable);
        return PageResponse.fromPage(page.map(BookingDto::fromEntity));
    }

    @Override
    public BookingDto approveBooking(Long id, BookingApproveRequest request, Long reviewerId, Role reviewerRole) {
        if (reviewerRole == Role.FACULTY && !facultyApprovalEnabled) {
            throw new AccessDeniedException("Faculty approval is not permitted by current booking policy");
        }

        Booking booking = getEntityById(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Cannot approve booking with status: " + booking.getStatus());
        }

        // Lock resource row to ensure serializable consistency and avoid race conditions with concurrent reviews
        Resource resource = resourceRepository.findByIdWithLock(booking.getResource().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + booking.getResource().getId()));

        if (!resource.isActive() || resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new BadRequestException("Resource is currently " + resource.getStatus() + " and cannot be approved");
        }

        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                booking.getResource().getId(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                BookingStatus.APPROVED,
                booking.getId()
        );

        if (!conflicts.isEmpty()) {
            throw new ConflictException("Cannot approve: resource is already booked for this time slot by another approved request");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with id: " + reviewerId));

        booking.setStatus(BookingStatus.APPROVED);
        booking.setReviewedBy(reviewer);
        booking.setReviewedAt(java.time.LocalDateTime.now());
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            booking.setAdminRemarks(request.getRemarks().trim());
        }

        Booking saved = bookingRepository.save(booking);
        return BookingDto.fromEntity(saved);
    }

    @Override
    public BookingDto rejectBooking(Long id, BookingRejectRequest request, Long reviewerId, Role reviewerRole) {
        if (reviewerRole == Role.FACULTY && !facultyApprovalEnabled) {
            throw new AccessDeniedException("Faculty approval is not permitted by current booking policy");
        }

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
            throw new BadRequestException("Rejection reason is required");
        }

        Booking booking = getEntityById(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Cannot reject booking with status: " + booking.getStatus());
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with id: " + reviewerId));

        booking.setStatus(BookingStatus.REJECTED);
        booking.setReviewedBy(reviewer);
        booking.setReviewedAt(java.time.LocalDateTime.now());
        booking.setAdminRemarks(request.getRejectionReason().trim());

        Booking saved = bookingRepository.save(booking);
        return BookingDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingDto> getAllBookings(Long resourceId, Long userId, BookingStatus status, LocalDate date, Pageable pageable) {
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (resourceId != null) {
                predicates.add(cb.equal(root.get("resource").get("id"), resourceId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (date != null) {
                predicates.add(cb.equal(root.get("bookingDate"), date));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page.map(BookingDto::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getEntityById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private void validateBookingDateTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null) {
            throw new BadRequestException("Booking date is required");
        }
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time are required");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot book a resource for a past date");
        }
        if (date.isEqual(LocalDate.now()) && startTime.isBefore(LocalTime.now())) {
            throw new BadRequestException("Cannot book a resource for a past time slot");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("End time must be after start time");
        }
    }
}

package com.smartcollege.demo.service.impl;

import com.smartcollege.demo.dto.*;
import com.smartcollege.demo.entity.*;
import com.smartcollege.demo.exception.BadRequestException;
import com.smartcollege.demo.exception.ResourceNotFoundException;
import com.smartcollege.demo.repository.BookingRepository;
import com.smartcollege.demo.repository.UsageRecordRepository;
import com.smartcollege.demo.service.UsageRecordService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UsageRecordServiceImpl implements UsageRecordService {

    private final UsageRecordRepository usageRecordRepository;
    private final BookingRepository bookingRepository;

    public UsageRecordServiceImpl(UsageRecordRepository usageRecordRepository,
                                  BookingRepository bookingRepository) {
        this.usageRecordRepository = usageRecordRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public UsageRecordDto checkIn(CheckInRequest request, Long currentUserId, boolean isAdmin) {
        if (request == null || request.getBookingId() == null) {
            throw new BadRequestException("Booking ID is required for check-in");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

        if (!isAdmin && !booking.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to check in for another user's booking");
        }

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Cannot check in: Booking must be in APPROVED status (current: " + booking.getStatus() + ")");
        }

        Optional<UsageRecord> activeRecord = usageRecordRepository.findByBookingIdAndStatus(booking.getId(), UsageStatus.CHECKED_IN);
        if (activeRecord.isPresent()) {
            throw new BadRequestException("Booking already has an active check-in record (ID: " + activeRecord.get().getId() + ")");
        }

        UsageRecord record = new UsageRecord();
        record.setBooking(booking);
        record.setResource(booking.getResource());
        record.setUser(booking.getUser());
        record.setCheckInTime(LocalDateTime.now());
        record.setStatus(UsageStatus.CHECKED_IN);
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            record.setRemarks(request.getRemarks().trim());
        }

        UsageRecord saved = usageRecordRepository.save(record);
        return UsageRecordDto.fromEntity(saved);
    }

    @Override
    public UsageRecordDto checkOut(Long usageRecordId, CheckOutRequest request, Long currentUserId, boolean isAdmin) {
        UsageRecord record = getEntityById(usageRecordId);

        if (!isAdmin && !record.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to check out this usage record");
        }

        if (record.getStatus() != UsageStatus.CHECKED_IN) {
            throw new BadRequestException("Cannot check out: record is not currently checked in (status: " + record.getStatus() + ")");
        }

        record.setCheckOutTime(LocalDateTime.now());
        record.setStatus(UsageStatus.COMPLETED);
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            String existing = record.getRemarks();
            String newRemark = request.getRemarks().trim();
            record.setRemarks(existing == null ? newRemark : existing + " | Check-out: " + newRemark);
        }

        // Advance booking status to COMPLETED if linked to a booking
        if (record.getBooking() != null) {
            Booking booking = record.getBooking();
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
        }

        UsageRecord saved = usageRecordRepository.save(record);
        return UsageRecordDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getMyUsageRecords(Long userId) {
        return usageRecordRepository.findByUserIdOrderByCheckInTimeDesc(userId).stream()
                .map(UsageRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsageRecordDto getUsageRecordById(Long id, Long currentUserId, boolean isPrivileged) {
        UsageRecord record = getEntityById(id);

        if (!isPrivileged && !record.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to view this usage record");
        }

        return UsageRecordDto.fromEntity(record);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsageRecordDto> getAllUsageRecords(Long resourceId, Long userId, UsageStatus status, Pageable pageable) {
        Specification<UsageRecord> spec = (root, query, cb) -> {
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UsageRecord> page = usageRecordRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page.map(UsageRecordDto::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getAllUsageRecords() {
        return usageRecordRepository.findAll().stream()
                .map(UsageRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsageRecordDto getUsageRecordById(Long id) {
        return UsageRecordDto.fromEntity(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getUsageRecordsByResourceId(Long resourceId) {
        return usageRecordRepository.findByResourceId(resourceId).stream()
                .map(UsageRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getUsageRecordsByStatus(UsageStatus status) {
        return usageRecordRepository.findByStatus(status).stream()
                .map(UsageRecordDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsageRecord getEntityById(Long id) {
        return usageRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usage record not found with id: " + id));
    }
}

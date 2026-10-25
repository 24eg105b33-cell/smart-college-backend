package com.smartcollege.demo.repository;

import com.smartcollege.demo.entity.Booking;
import com.smartcollege.demo.entity.BookingStatus;
import com.smartcollege.demo.entity.ResourceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    List<Booking> findByUserIdOrderByBookingDateDescStartTimeDesc(Long userId);

    List<Booking> findByResourceId(Long resourceId);

    List<Booking> findByStatus(BookingStatus status);

    long countByStatus(BookingStatus status);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    List<Booking> findByResourceIdAndBookingDate(Long resourceId, LocalDate bookingDate);

    List<Booking> findByResourceIdAndBookingDateAndStatusOrderByStartTimeAsc(
            Long resourceId, LocalDate bookingDate, BookingStatus status);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.resource.id = :resourceId
          AND b.bookingDate = :bookingDate
          AND b.status = :status
          AND b.startTime < :endTime
          AND b.endTime > :startTime
          AND (:excludeBookingId IS NULL OR b.id != :excludeBookingId)
    """)
    List<Booking> findOverlappingBookings(
            @Param("resourceId") Long resourceId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("status") BookingStatus status,
            @Param("excludeBookingId") Long excludeBookingId
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.resource r
        JOIN FETCH b.user u
        WHERE b.bookingDate BETWEEN :startDate AND :endDate
          AND (:category IS NULL OR r.category = :category)
          AND (:resourceId IS NULL OR r.id = :resourceId)
        ORDER BY b.bookingDate ASC, b.startTime ASC
    """)
    List<Booking> findReportBookings(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") ResourceCategory category,
            @Param("resourceId") Long resourceId
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.resource r
        JOIN FETCH b.user u
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findRecentActivity(Pageable pageable);
}

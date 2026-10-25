package com.smartcollege.demo.repository;

import com.smartcollege.demo.entity.UsageRecord;
import com.smartcollege.demo.entity.UsageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long>, JpaSpecificationExecutor<UsageRecord> {

    List<UsageRecord> findByResourceId(Long resourceId);

    List<UsageRecord> findByUserId(Long userId);

    List<UsageRecord> findByUserIdOrderByCheckInTimeDesc(Long userId);

    List<UsageRecord> findByStatus(UsageStatus status);

    List<UsageRecord> findByBookingId(Long bookingId);

    Optional<UsageRecord> findByBookingIdAndStatus(Long bookingId, UsageStatus status);
}

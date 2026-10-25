package com.smartcollege.demo.service;

import com.smartcollege.demo.dto.CheckInRequest;
import com.smartcollege.demo.dto.CheckOutRequest;
import com.smartcollege.demo.dto.PageResponse;
import com.smartcollege.demo.dto.UsageRecordDto;
import com.smartcollege.demo.entity.UsageRecord;
import com.smartcollege.demo.entity.UsageStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UsageRecordService {

    UsageRecordDto checkIn(CheckInRequest request, Long currentUserId, boolean isAdmin);

    UsageRecordDto checkOut(Long usageRecordId, CheckOutRequest request, Long currentUserId, boolean isAdmin);

    List<UsageRecordDto> getMyUsageRecords(Long userId);

    UsageRecordDto getUsageRecordById(Long id, Long currentUserId, boolean isPrivileged);

    PageResponse<UsageRecordDto> getAllUsageRecords(Long resourceId, Long userId, UsageStatus status, Pageable pageable);

    List<UsageRecordDto> getAllUsageRecords();

    UsageRecordDto getUsageRecordById(Long id);

    List<UsageRecordDto> getUsageRecordsByResourceId(Long resourceId);

    List<UsageRecordDto> getUsageRecordsByStatus(UsageStatus status);

    UsageRecord getEntityById(Long id);
}

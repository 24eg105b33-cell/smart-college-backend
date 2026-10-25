package com.smartcollege.demo.dto;

import com.smartcollege.demo.entity.UsageRecord;
import com.smartcollege.demo.entity.UsageStatus;

import java.time.LocalDateTime;

public class UsageRecordDto {

    private Long id;
    private Long bookingId;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String userName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private UsageStatus status;
    private String remarks;
    private LocalDateTime createdAt;

    public UsageRecordDto() {
    }

    public static UsageRecordDto fromEntity(UsageRecord record) {
        if (record == null) return null;
        UsageRecordDto dto = new UsageRecordDto();
        dto.setId(record.getId());
        if (record.getBooking() != null) {
            dto.setBookingId(record.getBooking().getId());
        }
        if (record.getResource() != null) {
            dto.setResourceId(record.getResource().getId());
            dto.setResourceName(record.getResource().getName());
        }
        if (record.getUser() != null) {
            dto.setUserId(record.getUser().getId());
            dto.setUserName(record.getUser().getFullName());
        }
        dto.setCheckInTime(record.getCheckInTime());
        dto.setCheckOutTime(record.getCheckOutTime());
        dto.setStatus(record.getStatus());
        dto.setRemarks(record.getRemarks());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public UsageStatus getStatus() {
        return status;
    }

    public void setStatus(UsageStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

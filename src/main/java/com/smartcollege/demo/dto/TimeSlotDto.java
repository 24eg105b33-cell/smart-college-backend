package com.smartcollege.demo.dto;

import com.smartcollege.demo.entity.BookingStatus;

import java.time.LocalTime;

public class TimeSlotDto {

    private Long bookingId;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private String purpose;

    public TimeSlotDto() {
    }

    public TimeSlotDto(Long bookingId, LocalTime startTime, LocalTime endTime, BookingStatus status, String purpose) {
        this.bookingId = bookingId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.purpose = purpose;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}

package com.smartcollege.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CheckInRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    public CheckInRequest() {
    }

    public CheckInRequest(Long bookingId, String remarks) {
        this.bookingId = bookingId;
        this.remarks = remarks;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

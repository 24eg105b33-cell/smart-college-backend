package com.smartcollege.demo.dto;

import com.smartcollege.demo.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BookingReviewRequest {

    @NotNull(message = "Review status is required")
    private BookingStatus status;

    @Size(max = 500, message = "Admin remarks must not exceed 500 characters")
    private String adminRemarks;

    public BookingReviewRequest() {
    }

    public BookingReviewRequest(BookingStatus status, String adminRemarks) {
        this.status = status;
        this.adminRemarks = adminRemarks;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }
}

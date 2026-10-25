package com.smartcollege.demo.dto;

import jakarta.validation.constraints.Size;

public class BookingApproveRequest {

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    public BookingApproveRequest() {
    }

    public BookingApproveRequest(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

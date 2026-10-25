package com.smartcollege.demo.dto;

import jakarta.validation.constraints.Size;

public class CheckOutRequest {

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    public CheckOutRequest() {
    }

    public CheckOutRequest(String remarks) {
        this.remarks = remarks;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

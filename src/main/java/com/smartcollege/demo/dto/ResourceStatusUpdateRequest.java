package com.smartcollege.demo.dto;

import com.smartcollege.demo.entity.ResourceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResourceStatusUpdateRequest {

    @NotNull(message = "Resource status is required")
    private ResourceStatus status;

    private Boolean active;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    public ResourceStatusUpdateRequest() {
    }

    public ResourceStatusUpdateRequest(ResourceStatus status, Boolean active, String remarks) {
        this.status = status;
        this.active = active;
        this.remarks = remarks;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

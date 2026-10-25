package com.smartcollege.demo.dto;

import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.entity.ResourceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ResourceRequest {

    @NotBlank(message = "Resource code is required")
    @Size(max = 50, message = "Resource code must not exceed 50 characters")
    private String resourceCode;

    @NotBlank(message = "Resource name is required")
    @Size(max = 150, message = "Resource name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Resource category is required")
    private ResourceCategory category;

    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @PositiveOrZero(message = "Capacity must be greater than or equal to 0")
    private Integer capacity;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private ResourceStatus status;

    private Boolean active;

    public ResourceRequest() {
    }

    public ResourceRequest(String resourceCode, String name, ResourceCategory category, String location, Integer capacity, String description, ResourceStatus status) {
        this.resourceCode = resourceCode;
        this.name = name;
        this.category = category;
        this.location = location;
        this.capacity = capacity;
        this.description = description;
        this.status = status;
        this.active = true;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public void setCategory(ResourceCategory category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}

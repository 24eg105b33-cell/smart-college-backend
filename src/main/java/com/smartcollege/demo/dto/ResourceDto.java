package com.smartcollege.demo.dto;

import com.smartcollege.demo.entity.Resource;
import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.entity.ResourceStatus;

import java.time.LocalDateTime;

public class ResourceDto {

    private Long id;
    private String resourceCode;
    private String name;
    private ResourceCategory category;
    private String location;
    private Integer capacity;
    private String description;
    private ResourceStatus status;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ResourceDto() {
    }

    public static ResourceDto fromEntity(Resource resource) {
        if (resource == null) return null;
        ResourceDto dto = new ResourceDto();
        dto.setId(resource.getId());
        dto.setResourceCode(resource.getResourceCode());
        dto.setName(resource.getName());
        dto.setCategory(resource.getCategory());
        dto.setLocation(resource.getLocation());
        dto.setCapacity(resource.getCapacity());
        dto.setDescription(resource.getDescription());
        dto.setStatus(resource.getStatus());
        dto.setActive(resource.isActive());
        dto.setCreatedAt(resource.getCreatedAt());
        dto.setUpdatedAt(resource.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

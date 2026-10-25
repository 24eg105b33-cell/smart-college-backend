package com.smartcollege.demo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ResourceAvailabilityDto {

    private Long resourceId;
    private String resourceName;
    private String resourceCode;
    private LocalDate date;
    private boolean available;
    private String message;
    private List<TimeSlotDto> bookedSlots = new ArrayList<>();

    public ResourceAvailabilityDto() {
    }

    public ResourceAvailabilityDto(Long resourceId, String resourceName, String resourceCode, LocalDate date, boolean available, String message, List<TimeSlotDto> bookedSlots) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceCode = resourceCode;
        this.date = date;
        this.available = available;
        this.message = message;
        this.bookedSlots = bookedSlots != null ? bookedSlots : new ArrayList<>();
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

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TimeSlotDto> getBookedSlots() {
        return bookedSlots;
    }

    public void setBookedSlots(List<TimeSlotDto> bookedSlots) {
        this.bookedSlots = bookedSlots;
    }
}

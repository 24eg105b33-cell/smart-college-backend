package com.smartcollege.demo.dto;

public class CategorySummaryDto {

    private String category;
    private String displayName;
    private long totalResources;
    private long availableResources;
    private long totalBookings;
    private long approvedBookings;
    private double bookedHours;
    private double utilizationRate;

    public CategorySummaryDto() {
    }

    public CategorySummaryDto(String category, String displayName, long totalResources, long availableResources, long totalBookings, long approvedBookings, double bookedHours, double utilizationRate) {
        this.category = category;
        this.displayName = displayName;
        this.totalResources = totalResources;
        this.availableResources = availableResources;
        this.totalBookings = totalBookings;
        this.approvedBookings = approvedBookings;
        this.bookedHours = bookedHours;
        this.utilizationRate = utilizationRate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getTotalResources() {
        return totalResources;
    }

    public void setTotalResources(long totalResources) {
        this.totalResources = totalResources;
    }

    public long getAvailableResources() {
        return availableResources;
    }

    public void setAvailableResources(long availableResources) {
        this.availableResources = availableResources;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getApprovedBookings() {
        return approvedBookings;
    }

    public void setApprovedBookings(long approvedBookings) {
        this.approvedBookings = approvedBookings;
    }

    public double getBookedHours() {
        return bookedHours;
    }

    public void setBookedHours(double bookedHours) {
        this.bookedHours = bookedHours;
    }

    public double getUtilizationRate() {
        return utilizationRate;
    }

    public void setUtilizationRate(double utilizationRate) {
        this.utilizationRate = utilizationRate;
    }
}

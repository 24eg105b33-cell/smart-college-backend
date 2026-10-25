package com.smartcollege.demo.dto;

public class TopResourceStatDto {

    private Long resourceId;
    private String name;
    private String resourceCode;
    private String category;
    private long count;
    private double bookedHours;
    private double utilizationRate;

    public TopResourceStatDto() {
    }

    public TopResourceStatDto(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public TopResourceStatDto(Long resourceId, String name, String resourceCode, String category, long count, double bookedHours, double utilizationRate) {
        this.resourceId = resourceId;
        this.name = name;
        this.resourceCode = resourceCode;
        this.category = category;
        this.count = count;
        this.bookedHours = bookedHours;
        this.utilizationRate = utilizationRate;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
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

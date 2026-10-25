package com.smartcollege.demo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportSummaryDto {

    private LocalDate startDate;
    private LocalDate endDate;

    // Resource KPIs
    private long totalResources;
    private long totalActiveResources;
    private long availableResources;

    // Booking KPIs
    private long totalBookings;
    private long approvedBookings;
    private long completedBookings;
    private long pendingBookings;
    private long rejectedBookings;
    private long cancelledBookings;
    private long pendingApprovalsCount;

    // Utilization KPIs
    private long totalUsageSessions;
    private int availabilityRate;
    private int approvalRate;
    private double totalOperationalHours;
    private double totalBookedHours;
    private double utilizationRate;
    private double averageBookingDurationHours;

    // Detailed collections
    private Map<String, Long> categoryBreakdown = new HashMap<>();
    private List<CategorySummaryDto> categorySummaries = new ArrayList<>();
    private List<TopResourceStatDto> topResources = new ArrayList<>();
    private List<BookingTrendDto> trends = new ArrayList<>();
    private List<RecentActivityDto> recentActivity = new ArrayList<>();

    public ReportSummaryDto() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getTotalResources() {
        return totalResources;
    }

    public void setTotalResources(long totalResources) {
        this.totalResources = totalResources;
    }

    public long getTotalActiveResources() {
        return totalActiveResources;
    }

    public void setTotalActiveResources(long totalActiveResources) {
        this.totalActiveResources = totalActiveResources;
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

    public long getCompletedBookings() {
        return completedBookings;
    }

    public void setCompletedBookings(long completedBookings) {
        this.completedBookings = completedBookings;
    }

    public long getPendingBookings() {
        return pendingBookings;
    }

    public void setPendingBookings(long pendingBookings) {
        this.pendingBookings = pendingBookings;
    }

    public long getRejectedBookings() {
        return rejectedBookings;
    }

    public void setRejectedBookings(long rejectedBookings) {
        this.rejectedBookings = rejectedBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public long getPendingApprovalsCount() {
        return pendingApprovalsCount;
    }

    public void setPendingApprovalsCount(long pendingApprovalsCount) {
        this.pendingApprovalsCount = pendingApprovalsCount;
    }

    public long getTotalUsageSessions() {
        return totalUsageSessions;
    }

    public void setTotalUsageSessions(long totalUsageSessions) {
        this.totalUsageSessions = totalUsageSessions;
    }

    public int getAvailabilityRate() {
        return availabilityRate;
    }

    public void setAvailabilityRate(int availabilityRate) {
        this.availabilityRate = availabilityRate;
    }

    public int getApprovalRate() {
        return approvalRate;
    }

    public void setApprovalRate(int approvalRate) {
        this.approvalRate = approvalRate;
    }

    public double getTotalOperationalHours() {
        return totalOperationalHours;
    }

    public void setTotalOperationalHours(double totalOperationalHours) {
        this.totalOperationalHours = totalOperationalHours;
    }

    public double getTotalBookedHours() {
        return totalBookedHours;
    }

    public void setTotalBookedHours(double totalBookedHours) {
        this.totalBookedHours = totalBookedHours;
    }

    public double getUtilizationRate() {
        return utilizationRate;
    }

    public void setUtilizationRate(double utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    public double getAverageBookingDurationHours() {
        return averageBookingDurationHours;
    }

    public void setAverageBookingDurationHours(double averageBookingDurationHours) {
        this.averageBookingDurationHours = averageBookingDurationHours;
    }

    public Map<String, Long> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(Map<String, Long> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<CategorySummaryDto> getCategorySummaries() {
        return categorySummaries;
    }

    public void setCategorySummaries(List<CategorySummaryDto> categorySummaries) {
        this.categorySummaries = categorySummaries;
    }

    public List<TopResourceStatDto> getTopResources() {
        return topResources;
    }

    public void setTopResources(List<TopResourceStatDto> topResources) {
        this.topResources = topResources;
    }

    public List<BookingTrendDto> getTrends() {
        return trends;
    }

    public void setTrends(List<BookingTrendDto> trends) {
        this.trends = trends;
    }

    public List<RecentActivityDto> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<RecentActivityDto> recentActivity) {
        this.recentActivity = recentActivity;
    }
}

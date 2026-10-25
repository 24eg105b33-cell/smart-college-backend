package com.smartcollege.demo.service.impl;

import com.smartcollege.demo.dto.*;
import com.smartcollege.demo.entity.*;
import com.smartcollege.demo.repository.BookingRepository;
import com.smartcollege.demo.repository.ResourceRepository;
import com.smartcollege.demo.repository.UsageRecordRepository;
import com.smartcollege.demo.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;
    private final UsageRecordRepository usageRecordRepository;

    public ReportServiceImpl(ResourceRepository resourceRepository,
                             BookingRepository bookingRepository,
                             UsageRecordRepository usageRecordRepository) {
        this.resourceRepository = resourceRepository;
        this.bookingRepository = bookingRepository;
        this.usageRecordRepository = usageRecordRepository;
    }

    @Override
    public ReportSummaryDto getSummaryReport() {
        LocalDate defaultEnd = LocalDate.now();
        LocalDate defaultStart = defaultEnd.minusDays(29);
        return getAnalyticsReport(defaultStart, defaultEnd, null, null);
    }

    @Override
    public ReportSummaryDto getAnalyticsReport(LocalDate startDate, LocalDate endDate, ResourceCategory category, Long resourceId) {
        ReportSummaryDto dto = new ReportSummaryDto();

        // 1. Sanitize date range
        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(29);
        } else if (startDate == null) {
            startDate = endDate.minusDays(29);
        } else if (endDate == null) {
            endDate = startDate.plusDays(29);
        }

        if (startDate.isAfter(endDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        dto.setStartDate(startDate);
        dto.setEndDate(endDate);

        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysInRange <= 0) {
            daysInRange = 1;
        }

        // 2. Query Active Resources according to filters
        List<Resource> activeResources;
        if (resourceId != null) {
            Optional<Resource> resOpt = resourceRepository.findById(resourceId);
            if (resOpt.isPresent() && resOpt.get().isActive()) {
                activeResources = List.of(resOpt.get());
            } else {
                activeResources = Collections.emptyList();
            }
        } else if (category != null) {
            activeResources = resourceRepository.findByActiveTrueAndCategory(category);
        } else {
            activeResources = resourceRepository.findByActiveTrue();
        }

        long totalAllResources = resourceRepository.count();
        long totalActiveResources = activeResources.size();
        long availableResources = activeResources.stream()
                .filter(r -> r.getStatus() == ResourceStatus.AVAILABLE)
                .count();

        dto.setTotalResources(totalAllResources);
        dto.setTotalActiveResources(totalActiveResources);
        dto.setAvailableResources(availableResources);

        // Operational capacity: standard 12 operational hours per day per active resource
        double totalOperationalHours = totalActiveResources * daysInRange * 12.0;
        dto.setTotalOperationalHours(totalOperationalHours);

        // 3. Query Filtered Bookings for the date range
        List<Booking> bookings = bookingRepository.findReportBookings(startDate, endDate, category, resourceId);

        long totalBookings = bookings.size();
        long approvedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.APPROVED).count();
        long completedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long pendingBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long rejectedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.REJECTED).count();
        long cancelledBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long pendingApprovalsCount = bookingRepository.countByStatus(BookingStatus.PENDING);
        long totalUsage = usageRecordRepository.count();

        dto.setTotalBookings(totalBookings);
        dto.setApprovedBookings(approvedBookings);
        dto.setCompletedBookings(completedBookings);
        dto.setPendingBookings(pendingBookings);
        dto.setRejectedBookings(rejectedBookings);
        dto.setCancelledBookings(cancelledBookings);
        dto.setPendingApprovalsCount(pendingApprovalsCount);
        dto.setTotalUsageSessions(totalUsage);

        // 4. Calculate Non-Overlapping Booked Hours (Requirement 11)
        List<Booking> utilizedBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.COMPLETED)
                .toList();

        double totalBookedHours = calculateNonOverlappingHours(utilizedBookings);
        dto.setTotalBookedHours(totalBookedHours);

        // 5. Utilization and safe rate calculations (Requirement 12)
        double utilizationRate = 0.0;
        if (totalOperationalHours > 0) {
            utilizationRate = Math.round((totalBookedHours / totalOperationalHours) * 1000.0) / 10.0;
            if (utilizationRate > 100.0) {
                utilizationRate = 100.0;
            }
        }
        dto.setUtilizationRate(utilizationRate);

        int availabilityRate = totalActiveResources > 0
                ? (int) Math.round(((double) availableResources / totalActiveResources) * 100)
                : 100;
        int approvalRate = totalBookings > 0
                ? (int) Math.round(((double) (approvedBookings + completedBookings) / totalBookings) * 100)
                : 100;
        dto.setAvailabilityRate(availabilityRate);
        dto.setApprovalRate(approvalRate);

        long utilizedCount = approvedBookings + completedBookings;
        double avgDuration = utilizedCount > 0
                ? Math.round((totalBookedHours / utilizedCount) * 10.0) / 10.0
                : 0.0;
        dto.setAverageBookingDurationHours(avgDuration);

        // 6. Category Breakdown & Summaries (Requirement 6)
        Map<String, Long> categoryCountMap = new HashMap<>();
        List<Resource> allActiveForCat = resourceRepository.findByActiveTrue();
        for (Resource r : allActiveForCat) {
            String catName = r.getCategory() != null ? r.getCategory().name() : "OTHER";
            categoryCountMap.put(catName, categoryCountMap.getOrDefault(catName, 0L) + 1);
        }
        dto.setCategoryBreakdown(categoryCountMap);

        List<CategorySummaryDto> catSummaries = new ArrayList<>();
        Map<ResourceCategory, List<Booking>> bookingsByCat = bookings.stream()
                .filter(b -> b.getResource() != null && b.getResource().getCategory() != null)
                .collect(Collectors.groupingBy(b -> b.getResource().getCategory()));

        for (ResourceCategory cat : ResourceCategory.values()) {
            List<Resource> catResources = allActiveForCat.stream()
                    .filter(r -> r.getCategory() == cat)
                    .toList();
            long catResCount = catResources.size();
            long catAvailCount = catResources.stream().filter(r -> r.getStatus() == ResourceStatus.AVAILABLE).count();

            List<Booking> catBookings = bookingsByCat.getOrDefault(cat, Collections.emptyList());
            long catBookingCount = catBookings.size();
            long catApprovedCount = catBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.COMPLETED)
                    .count();

            List<Booking> catUtilized = catBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.COMPLETED)
                    .toList();
            double catBookedHours = calculateNonOverlappingHours(catUtilized);

            double catOpHours = catResCount * daysInRange * 12.0;
            double catUtilRate = catOpHours > 0
                    ? Math.min(100.0, Math.round((catBookedHours / catOpHours) * 1000.0) / 10.0)
                    : 0.0;

            String displayName = formatCategoryDisplayName(cat);
            catSummaries.add(new CategorySummaryDto(
                    cat.name(),
                    displayName,
                    catResCount,
                    catAvailCount,
                    catBookingCount,
                    catApprovedCount,
                    catBookedHours,
                    catUtilRate
            ));
        }
        dto.setCategorySummaries(catSummaries);

        // 7. Top Booked Resources (Requirement 4)
        Map<Long, List<Booking>> bookingsByResource = bookings.stream()
                .filter(b -> b.getResource() != null)
                .collect(Collectors.groupingBy(b -> b.getResource().getId()));

        List<TopResourceStatDto> topResources = new ArrayList<>();
        for (Map.Entry<Long, List<Booking>> entry : bookingsByResource.entrySet()) {
            List<Booking> resBookings = entry.getValue();
            if (resBookings.isEmpty()) continue;
            Resource r = resBookings.get(0).getResource();

            List<Booking> resUtilized = resBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.COMPLETED)
                    .toList();
            double resHours = calculateNonOverlappingHours(resUtilized);
            double resOpHours = daysInRange * 12.0;
            double resUtilRate = resOpHours > 0
                    ? Math.min(100.0, Math.round((resHours / resOpHours) * 1000.0) / 10.0)
                    : 0.0;

            topResources.add(new TopResourceStatDto(
                    r.getId(),
                    r.getName(),
                    r.getResourceCode(),
                    r.getCategory() != null ? r.getCategory().name() : "OTHER",
                    resBookings.size(),
                    resHours,
                    resUtilRate
            ));
        }

        // Sort descending by booking count, then booked hours
        topResources.sort(Comparator.comparingLong(TopResourceStatDto::getCount)
                .thenComparingDouble(TopResourceStatDto::getBookedHours).reversed());
        if (topResources.size() > 10) {
            topResources = topResources.subList(0, 10);
        }
        dto.setTopResources(topResources);

        // 8. Daily Booking Trends (Filling missing dates with 0, Requirement 5 & 12)
        Map<LocalDate, List<Booking>> bookingsByDate = bookings.stream()
                .filter(b -> b.getBookingDate() != null)
                .collect(Collectors.groupingBy(Booking::getBookingDate));

        List<BookingTrendDto> trendList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Booking> dayList = bookingsByDate.getOrDefault(date, Collections.emptyList());
            int dayTotal = dayList.size();
            int dayApproved = (int) dayList.stream().filter(b -> b.getStatus() == BookingStatus.APPROVED).count();
            int dayPending = (int) dayList.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
            int dayRejected = (int) dayList.stream().filter(b -> b.getStatus() == BookingStatus.REJECTED).count();
            int dayCancelled = (int) dayList.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
            int dayCompleted = (int) dayList.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

            List<Booking> dayUtilized = dayList.stream()
                    .filter(b -> b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.COMPLETED)
                    .toList();
            double dayHours = calculateNonOverlappingHours(dayUtilized);

            trendList.add(new BookingTrendDto(
                    date.toString(),
                    dayTotal,
                    dayApproved,
                    dayPending,
                    dayRejected,
                    dayCancelled,
                    dayCompleted,
                    dayHours
            ));
        }
        dto.setTrends(trendList);

        // 9. Recent Activity (Latest 10 bookings, Requirement 7)
        List<Booking> recentList = bookingRepository.findRecentActivity(PageRequest.of(0, 10));
        List<RecentActivityDto> activityDtos = recentList.stream().map(b -> new RecentActivityDto(
                b.getId(),
                b.getResource() != null ? b.getResource().getId() : null,
                b.getResource() != null ? b.getResource().getName() : "Unknown Resource",
                b.getResource() != null ? b.getResource().getResourceCode() : "N/A",
                b.getUser() != null ? b.getUser().getFullName() : "Unknown User",
                b.getUser() != null ? b.getUser().getEmail() : "N/A",
                b.getUser() != null ? b.getUser().getDepartment() : "N/A",
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getPurpose(),
                b.getStatus(),
                b.getCreatedAt()
        )).collect(Collectors.toList());
        dto.setRecentActivity(activityDtos);

        return dto;
    }

    /**
     * Calculates non-overlapping utilized hours across bookings.
     * Groups intervals by (resourceId, bookingDate), merges overlapping/contiguous intervals,
     * and sums the merged durations to prevent any double counting. (Requirement 11)
     */
    private double calculateNonOverlappingHours(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return 0.0;
        }

        // Group by resource and date
        Map<String, List<Booking>> grouped = bookings.stream()
                .filter(b -> b.getResource() != null && b.getBookingDate() != null
                        && b.getStartTime() != null && b.getEndTime() != null)
                .collect(Collectors.groupingBy(b -> b.getResource().getId() + "_" + b.getBookingDate()));

        long totalMinutes = 0;

        for (List<Booking> groupBookings : grouped.values()) {
            List<int[]> intervals = new ArrayList<>();
            for (Booking b : groupBookings) {
                int startMinute = b.getStartTime().getHour() * 60 + b.getStartTime().getMinute();
                int endMinute = b.getEndTime().getHour() * 60 + b.getEndTime().getMinute();
                if (endMinute > startMinute) {
                    intervals.add(new int[]{startMinute, endMinute});
                }
            }

            // Sort by start minute
            intervals.sort(Comparator.comparingInt(a -> a[0]));

            // Merge overlapping or adjacent intervals
            List<int[]> merged = new ArrayList<>();
            for (int[] current : intervals) {
                if (merged.isEmpty()) {
                    merged.add(current);
                } else {
                    int[] last = merged.get(merged.size() - 1);
                    if (current[0] <= last[1]) {
                        // Overlapping or adjacent: merge
                        last[1] = Math.max(last[1], current[1]);
                    } else {
                        merged.add(current);
                    }
                }
            }

            // Sum durations of merged intervals
            for (int[] interval : merged) {
                totalMinutes += (interval[1] - interval[0]);
            }
        }

        return Math.round((totalMinutes / 60.0) * 10.0) / 10.0;
    }

    private String formatCategoryDisplayName(ResourceCategory cat) {
        return switch (cat) {
            case CLASSROOM -> "Classroom";
            case LABORATORY -> "Laboratory";
            case SEMINAR_HALL -> "Seminar Hall";
            case PROJECTOR -> "Projector / AV System";
            case COMPUTER -> "Computer Workstation";
            case LIBRARY -> "Library Facility";
            case SPORTS -> "Sports Complex";
            case EQUIPMENT -> "General Equipment";
        };
    }
}

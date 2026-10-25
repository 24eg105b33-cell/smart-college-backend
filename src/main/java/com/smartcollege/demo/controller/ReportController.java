package com.smartcollege.demo.controller;

import com.smartcollege.demo.dto.ApiResponse;
import com.smartcollege.demo.dto.RecentActivityDto;
import com.smartcollege.demo.dto.ReportSummaryDto;
import com.smartcollege.demo.entity.ResourceCategory;
import com.smartcollege.demo.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<ApiResponse<ReportSummaryDto>> getSummaryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ResourceCategory category,
            @RequestParam(required = false) Long resourceId) {

        ReportSummaryDto summary = reportService.getAnalyticsReport(startDate, endDate, category, resourceId);
        return ResponseEntity.ok(ApiResponse.success("Reports and analytics retrieved successfully", summary));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<ApiResponse<List<RecentActivityDto>>> getRecentActivity() {
        ReportSummaryDto summary = reportService.getSummaryReport();
        return ResponseEntity.ok(ApiResponse.success("Recent activity retrieved successfully", summary.getRecentActivity()));
    }
}

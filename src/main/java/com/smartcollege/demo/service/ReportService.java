package com.smartcollege.demo.service;

import com.smartcollege.demo.dto.ReportSummaryDto;
import com.smartcollege.demo.entity.ResourceCategory;

import java.time.LocalDate;

public interface ReportService {

    ReportSummaryDto getSummaryReport();

    ReportSummaryDto getAnalyticsReport(LocalDate startDate, LocalDate endDate, ResourceCategory category, Long resourceId);
}

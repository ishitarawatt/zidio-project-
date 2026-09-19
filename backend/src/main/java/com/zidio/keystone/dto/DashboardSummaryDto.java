package com.zidio.keystone.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class DashboardSummaryDto {
    private Map<String, Long> countsByStatus;
    private long overdueCount;
    private double slaCompliancePercent;
    private Map<String, Long> openCountByTechnician;
    private Map<String, Long> openCountBySite;
}

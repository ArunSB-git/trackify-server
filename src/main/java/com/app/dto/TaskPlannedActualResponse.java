package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskPlannedActualResponse {
    private String monthKey;    // "2025-01"
    private String monthLabel;  // "January 2025"
    private long plannedDays;
    private long actualDays;
}
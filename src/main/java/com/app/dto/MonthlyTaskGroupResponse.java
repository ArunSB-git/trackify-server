package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MonthlyTaskGroupResponse {
    private String month; // e.g. "2025-01"
    private String monthLabel;  // "January 2025"
    private List<MonthlyTaskDetailResponse> tasks;
}
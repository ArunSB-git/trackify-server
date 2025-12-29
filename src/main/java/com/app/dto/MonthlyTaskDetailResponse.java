package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyTaskDetailResponse {
    private Integer taskId;
    private String taskTitle;
    private long completedDays;
}
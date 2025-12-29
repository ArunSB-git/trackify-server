package com.app.dto;



import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskStatsResponse {

    private Integer taskId;
    private String taskTitle;

    private long completedDaysCurrentMonth;
    private long totalCompletedDays;
    private double completionPercentageCurrentMonth;
    private double monthOverMonthChangePercentage;
}

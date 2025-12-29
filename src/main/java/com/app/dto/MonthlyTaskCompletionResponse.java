package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyTaskCompletionResponse {

    private String month;   // JANUARY, FEBRUARY, etc
    private long completedDays;
}
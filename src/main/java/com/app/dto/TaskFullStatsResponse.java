package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
public class TaskFullStatsResponse {

    // 1️⃣ Completion % current month
    private double completionPercentageCurrentMonth;

    // 2️⃣ % increase / decrease vs last month
    private double monthOverMonthChangePercentage;

    // 3️⃣ Total completed days (overall)
    private long totalCompletedDays;

    // 4️⃣ Days completed in current month
    private long completedDaysCurrentMonth;

    // 5️⃣ Current streak
    private int currentStreakDays;
    private LocalDate currentStreakFrom;
    private LocalDate currentStreakTo;

    // 6️⃣ Overall best streak
    private int bestStreakDays;
    private LocalDate bestStreakFrom;
    private LocalDate bestStreakTo;

    private Map<String, Long> frequencyPerWeekday;
}

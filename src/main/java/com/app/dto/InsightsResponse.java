package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InsightsResponse {

    private long totalCompletions;

    private HighestTaskCompletion highestTaskCompletions;

    private HighestStreak highestStreak;

    private long perfectDays;

    private long memberDays;

    private double consistencyPercentage;

    @Getter
    @AllArgsConstructor
    public static class HighestTaskCompletion {
        private Integer taskId;
        private String title;
        private long count;
    }

    @Getter
    @AllArgsConstructor
    public static class HighestStreak {
        private Integer taskId;
        private String title;
        private int streak;
    }
}

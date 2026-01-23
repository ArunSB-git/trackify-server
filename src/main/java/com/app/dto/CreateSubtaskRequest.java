package com.app.dto;

public record CreateSubtaskRequest(
        Integer taskId,
        String title
) {}

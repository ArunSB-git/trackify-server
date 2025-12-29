package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TaskResponse {
    private Integer id;
    private String title;
    private Boolean isActive;
    private Instant createdAt;
}
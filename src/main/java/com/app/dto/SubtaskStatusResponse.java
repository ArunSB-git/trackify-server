package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubtaskStatusResponse {
    private Integer subtaskId;
    private String title;
    private boolean completed;
}

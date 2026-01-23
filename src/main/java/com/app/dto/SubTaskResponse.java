package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Data
@AllArgsConstructor
public class SubTaskResponse {

    private Integer id;
    private String title;
    private Instant createdAt;

}

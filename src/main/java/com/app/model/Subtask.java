package com.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "subtasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "is_active")
    private Boolean isActive = true;
}

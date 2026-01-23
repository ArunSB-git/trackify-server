package com.app.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "subtask_completions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"subtask_id", "completed_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtaskCompletions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtask_id", nullable = false)
    private Subtask subtask;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}

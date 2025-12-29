package com.app.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "task_completions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"task_id", "completion_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCompletions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}

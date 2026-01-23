package com.app.repository;

import com.app.model.Subtask;
import com.app.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubtaskRepository extends JpaRepository<Subtask, Integer> {
    List<Subtask> findByTaskAndIsActiveTrueOrderByCreatedAtDesc(Task task);

    long countByTask(Task task);  // to check max 5 subtasks

}

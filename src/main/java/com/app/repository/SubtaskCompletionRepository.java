package com.app.repository;



import com.app.model.SubtaskCompletions;
import com.app.model.Subtask;
import com.app.model.Task;
import com.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubtaskCompletionRepository extends JpaRepository<SubtaskCompletions, Integer> {

    List<SubtaskCompletions> findBySubtask(Subtask subtask);

    List<SubtaskCompletions> findBySubtaskAndCompletedDateBetween(Subtask subtask, LocalDate start, LocalDate end);

    boolean existsBySubtaskAndCompletedDate(Subtask subtask, LocalDate date);

    Optional<SubtaskCompletions> findBySubtaskAndCompletedDate(Subtask subtask, LocalDate date);

    void deleteBySubtask(Subtask subtask);

    List<SubtaskCompletions> findBySubtaskTaskUser(User user);

    List<SubtaskCompletions> findBySubtaskTaskUserAndCompletedDateBetween(User user, LocalDate start, LocalDate end);

    List<SubtaskCompletions> findBySubtaskTaskAndCompletedDate(Task task, LocalDate date);

}

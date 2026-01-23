package com.app.repository;



import com.app.model.TaskCompletions;
import com.app.model.Task;
import com.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletions, Integer> {

    List<TaskCompletions> findByTaskAndCompletedDateBetween(Task task, LocalDate start, LocalDate end);

    List<TaskCompletions> findByTaskOrderByCompletedDateDesc(Task task);

    List<TaskCompletions> findByTask(Task task);

    boolean existsByTaskAndCompletedDate(Task task, LocalDate date);

    Optional<TaskCompletions> findByTaskAndCompletedDate(Task task, LocalDate date);

    long countByTaskAndCompletedDateBetween(
            Task task,
            LocalDate start,
            LocalDate end
    );

    long countByTask(Task task);

//    // For specific task
//    List<TaskCompletions> findByTaskAndCompletedDateBetween(
//            Task task,
//            LocalDate start,
//            LocalDate end
//    );

    // For all user tasks
    List<TaskCompletions> findByTaskUserAndCompletedDateBetween(
            User user,
            LocalDate start,
            LocalDate end
    );

    void deleteByTask(Task task);

    List<TaskCompletions> findByTaskUser(User user);

    long countByTaskUser(User user);


}

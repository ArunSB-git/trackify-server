package com.app.repository;


import com.app.model.Task;
import com.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByUserAndIsActiveTrue(User user);

    List<Task> findByUser(User user);

}

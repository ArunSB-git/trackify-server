package com.app.service;

import com.app.dto.SubTaskResponse;
import com.app.dto.SubtaskStatusResponse;
import com.app.model.Subtask;
import com.app.model.SubtaskCompletions;
import com.app.model.Task;
import com.app.model.User;
import com.app.repository.SubtaskCompletionRepository;
import com.app.repository.SubtaskRepository;
import com.app.repository.TaskRepository;
import com.app.config.SecurityUtil;
import com.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SubtaskCompletionRepository subtaskCompletionRepository;

    public SubtaskService(SubtaskRepository subtaskRepository,
                          TaskRepository taskRepository,
                          UserRepository userRepository,
                          SubtaskCompletionRepository subtaskCompletionRepository) {
        this.subtaskRepository = subtaskRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.subtaskCompletionRepository=subtaskCompletionRepository;
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email).orElseThrow();
    }

    @Transactional
    public Subtask createSubtask(Integer taskId, String title) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getHasSubtasks()) {
            throw new RuntimeException("Subtasks are not allowed for this task");
        }

        long subtaskCount = subtaskRepository.countByTask(task);
        if (subtaskCount >= 5) {
            throw new RuntimeException("Max no of sub tasks created for this task");
        }

        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setTitle(title);
        subtask.setCreatedAt(Instant.now());
        subtask.setIsActive(true);

        return subtaskRepository.save(subtask);
    }

    public List<SubTaskResponse> getSubtasks(Integer taskId) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        return subtaskRepository.findByTaskAndIsActiveTrueOrderByCreatedAtDesc(task)
                .stream()
                .map(st -> new SubTaskResponse(
                        st.getId(),
                        st.getTitle(),
                        st.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public Subtask editSubtask(Integer subtaskId, String newTitle) {
        User user = getCurrentUser();

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .filter(st -> st.getTask().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Subtask not found"));

        if (newTitle != null && !newTitle.trim().isEmpty()) {
            subtask.setTitle(newTitle.trim());
        }

        return subtaskRepository.save(subtask);
    }

    @Transactional
    public void deleteSubtask(Integer subtaskId) {
        User user = getCurrentUser();

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .filter(st -> st.getTask().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Subtask not found"));

        // Delete all completions of this subtask
        subtaskCompletionRepository.deleteBySubtask(subtask);

        // Delete the subtask
        subtaskRepository.delete(subtask);
    }

    @Transactional
    public void toggleSubtaskCompleted(Integer subtaskId, LocalDate date) {
        User user = getCurrentUser();

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .filter(st -> st.getTask().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Subtask not found"));

        if (subtaskCompletionRepository.existsBySubtaskAndCompletedDate(subtask, date)) {
            // undo completion
            subtaskCompletionRepository.findBySubtaskAndCompletedDate(subtask, date)
                    .ifPresent(subtaskCompletionRepository::delete);
        } else {
            // mark as completed
            SubtaskCompletions sc = new SubtaskCompletions();
            sc.setSubtask(subtask);
            sc.setCompletedDate(date);
            subtaskCompletionRepository.save(sc);
        }
    }

    // ---------------- Mark Completed ----------------
    @Transactional
    public void markCompleted(Integer subtaskId, LocalDate date) {
        User user = getCurrentUser();

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .filter(st -> st.getTask().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Subtask not found"));

        subtaskCompletionRepository.findBySubtaskAndCompletedDate(subtask, date)
                .orElseGet(() -> {
                    SubtaskCompletions sc = new SubtaskCompletions();
                    sc.setSubtask(subtask);
                    sc.setCompletedDate(date);
                    return subtaskCompletionRepository.save(sc);
                });
    }

    // ---------------- Undo Completed ----------------
    @Transactional
    public void undoCompleted(Integer subtaskId, LocalDate date) {
        User user = getCurrentUser();

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .filter(st -> st.getTask().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Subtask not found"));

        subtaskCompletionRepository.findBySubtaskAndCompletedDate(subtask, date)
                .ifPresent(subtaskCompletionRepository::delete);
    }

    // ---------------- Toggle Completed ----------------
    @Transactional
    public void toggleCompleted(Integer subtaskId, LocalDate date) {
        User user = getCurrentUser();

        Subtask subtask = subtaskRepository.findById(subtaskId)
                .filter(st -> st.getTask().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Subtask not found"));

        if (subtaskCompletionRepository.existsBySubtaskAndCompletedDate(subtask, date)) {
            undoCompleted(subtaskId, date);
        } else {
            markCompleted(subtaskId, date);
        }
    }

    public List<SubtaskStatusResponse> getSubtaskStatusByDate(Integer taskId, LocalDate date) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // 1️⃣ Get all active subtasks
        List<Subtask> subtasks =
                subtaskRepository.findByTaskAndIsActiveTrueOrderByCreatedAtDesc(task);

        // 2️⃣ Get completed subtasks for given date
        List<SubtaskCompletions> completedList =
                subtaskCompletionRepository.findBySubtaskTaskAndCompletedDate(task, date);

        // 3️⃣ Convert completed subtasks to Set for fast lookup
        var completedSubtaskIds = completedList.stream()
                .map(sc -> sc.getSubtask().getId())
                .collect(java.util.stream.Collectors.toSet());

        // 4️⃣ Build response
        return subtasks.stream()
                .map(st -> new SubtaskStatusResponse(
                        st.getId(),
                        st.getTitle(),
                        completedSubtaskIds.contains(st.getId())
                ))
                .toList();
    }


}

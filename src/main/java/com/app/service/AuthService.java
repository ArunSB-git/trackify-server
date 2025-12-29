package com.app.service;

import com.app.config.SecurityUtil;
import com.app.dto.UserProfileResponse;
import com.app.model.Task;
import com.app.model.User;
import com.app.repository.TaskCompletionRepository;
import com.app.repository.TaskRepository;
import com.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository completionRepository;

    public AuthService(UserRepository userRepository,
                       TaskRepository taskRepository,
                       TaskCompletionRepository completionRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.completionRepository = completionRepository;
    }

    @Transactional
    public void deleteUser(UUID userId) {

        // Step 1: Find all tasks of user
        List<Task> tasks = taskRepository.findByUserAndIsActiveTrue(
                userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );

        // Step 2: Delete all task completions of these tasks
        tasks.forEach(task -> completionRepository.deleteByTask(task));

        // Step 3: Delete all tasks
        taskRepository.deleteAll(tasks);

        // Step 4: Delete user
        userRepository.deleteById(userId);
    }

    public UserProfileResponse getCurrentUserProfile() {

        // 🔐 get email from security context
        String email = SecurityUtil.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getProfilePicture(),
                user.getCreatedAt()
        );
    }
}

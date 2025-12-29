package com.app.service;

import com.app.dto.TaskResponse;
import com.app.dto.TaskStatsResponse;
import com.app.model.Task;
import com.app.model.TaskCompletions;
import com.app.model.User;
import com.app.repository.TaskCompletionRepository;
import com.app.repository.TaskRepository;
import com.app.repository.UserRepository;
import com.app.config.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;


@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCompletionRepository completionRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            TaskCompletionRepository completionRepository,
            UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.completionRepository = completionRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email).orElseThrow();
    }

    public List<TaskResponse> getMyTasks() {
        return taskRepository.findByUserAndIsActiveTrue(getCurrentUser())
                .stream()
                .map(task -> new TaskResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getIsActive(),
                        task.getCreatedAt()
                ))
                .toList();
    }

    public Task createTask(String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setUser(getCurrentUser());
        return taskRepository.save(task);
    }

    public void undoCompleted(Integer taskId, LocalDate date) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        completionRepository.findByTaskAndCompletedDate(task, date)
                .ifPresent(completionRepository::delete);
    }

    public void markCompleted(Integer taskId, LocalDate date) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        completionRepository.findByTaskAndCompletedDate(task, date)
                .orElseGet(() -> {
                    TaskCompletions tc = new TaskCompletions();
                    tc.setTask(task);
                    tc.setCompletedDate(date);
                    return completionRepository.save(tc);
                });
    }

    public void toggleCompleted(Integer taskId, LocalDate date) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (completionRepository.existsByTaskAndCompletedDate(task, date)) {
            undoCompleted(taskId, date);
        } else {
            markCompleted(taskId, date);
        }
    }

    public int getTaskStreak(Integer taskId) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        List<TaskCompletions> completions =
                completionRepository.findByTaskOrderByCompletedDateDesc(task);

        int streak = 0;
        LocalDate expectedDate = LocalDate.now();

        for (TaskCompletions tc : completions) {
            if (tc.getCompletedDate().equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (tc.getCompletedDate().isBefore(expectedDate)) {
                break; // streak broken
            }
        }

        return streak;
    }

    public TaskStatsResponse getTaskStats(Integer taskId) {

        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // -------- Current Month --------
        YearMonth currentMonth = YearMonth.now();
        LocalDate cmStart = currentMonth.atDay(1);
        LocalDate cmEnd = currentMonth.atEndOfMonth();

        long completedCurrentMonth =
                completionRepository.countByTaskAndCompletedDateBetween(task, cmStart, cmEnd);

        int totalDaysCurrentMonth = currentMonth.lengthOfMonth();

        double completionPercentageCurrentMonth =
                totalDaysCurrentMonth == 0 ? 0 :
                        (completedCurrentMonth * 100.0) / totalDaysCurrentMonth;

        // -------- Previous Month --------
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate pmStart = previousMonth.atDay(1);
        LocalDate pmEnd = previousMonth.atEndOfMonth();

        long completedPreviousMonth =
                completionRepository.countByTaskAndCompletedDateBetween(task, pmStart, pmEnd);

        double monthOverMonthChangePercentage =
                completedPreviousMonth == 0
                        ? 0
                        : ((completedCurrentMonth - completedPreviousMonth) * 100.0) / completedPreviousMonth;

        // -------- Total Completed --------
        long totalCompletedDays = completionRepository.countByTask(task);

        return new TaskStatsResponse(
                task.getId(),
                task.getTitle(),
                completedCurrentMonth,
                totalCompletedDays,
                Math.round(completionPercentageCurrentMonth * 100.0) / 100.0,
                Math.round(monthOverMonthChangePercentage * 100.0) / 100.0
        );
    }

//    public Map<Integer, List<LocalDate>> getAllCompletedDates() {
//
//        User user = getCurrentUser();
//
//        return completionRepository.findByTaskUser(user)
//                .stream()
//                .collect(Collectors.groupingBy(
//                        tc -> tc.getTask().getId(),
//                        Collectors.mapping(
//                                TaskCompletions::getCompletedDate,
//                                Collectors.toList()
//                        )
//                ));
//    }

    public Map<Integer, List<LocalDate>> getAllCompletedDates(Integer taskId, String monthStr) {

        User user = getCurrentUser();
        LocalDate monthStart = null;
        LocalDate monthEnd = null;

        // If month param is provided
        if (monthStr != null && !monthStr.isBlank()) {
            YearMonth ym = YearMonth.parse(monthStr); // expects "yyyy-MM"
            monthStart = ym.atDay(1);
            monthEnd = ym.atEndOfMonth();
        }

        // If taskId is provided, get that specific task
        if (taskId != null) {
            Task task = taskRepository.findById(taskId)
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            List<TaskCompletions> completions;
            if (monthStart != null) {
                completions = completionRepository.findByTaskAndCompletedDateBetween(task, monthStart, monthEnd);
            } else {
                completions = completionRepository.findByTask(task);
            }

            return Map.of(
                    task.getId(),
                    completions.stream()
                            .map(TaskCompletions::getCompletedDate)
                            .toList()
            );
        }

        // No taskId provided, get all tasks of user
        List<TaskCompletions> completions;
        if (monthStart != null) {
            completions = completionRepository.findByTaskUserAndCompletedDateBetween(user, monthStart, monthEnd);
        } else {
            completions = completionRepository.findByTaskUser(user);
        }

        return completions.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getTask().getId(),
                        Collectors.mapping(TaskCompletions::getCompletedDate, Collectors.toList())
                ));
    }


    @Transactional
    public void deleteTask(Integer taskId) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // 1️⃣ Delete all completions of this task
        completionRepository.deleteByTask(task);

        // 2️⃣ Delete the task itself
        taskRepository.delete(task);
    }

    @Transactional
    public Task editTaskTitle(Integer taskId, String newTitle) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setTitle(newTitle.trim());
        return taskRepository.save(task); // save and return updated task
    }

    public void exportTasksCsv(HttpServletResponse response) throws IOException {
        User user = getCurrentUser();
        List<Task> tasks = taskRepository.findByUser(user);

        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=tasks.csv"
        );

        PrintWriter writer = response.getWriter();

        // CSV Header
        writer.println("id,title,created_at,is_active");

        // CSV Rows
        for (Task task : tasks) {
            writer.printf(
                    "%d,\"%s\",%b,%s%n",
                    task.getId(),
                    task.getTitle().replace("\"", "\"\""),
                    task.getCreatedAt(),
                    task.getIsActive()
            );
        }

        writer.flush();
    }

    public void exportTaskCompletionsCsv(HttpServletResponse response) throws IOException {
        User user = getCurrentUser();
        List<TaskCompletions> completions =
                completionRepository.findByTaskUser(user);

        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=task_completions.csv"
        );

        PrintWriter writer = response.getWriter();

        // CSV Header
        writer.println("id,task_id,completed_date,created_at");

        for (TaskCompletions tc : completions) {
            writer.printf(
                    "%d,%d,%s,%s%n",
                    tc.getId(),
                    tc.getTask().getId(),
                    tc.getCompletedDate(),
                    tc.getCreatedAt()
            );

        }

        writer.flush();
    }

    @Transactional
    public Map<Integer, Integer> importTasksCsv(MultipartFile file) throws IOException {

        User user = getCurrentUser();
        Map<Integer, Integer> taskIdMapping = new HashMap<>();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(file.getInputStream()));

        // ===== HEADER =====
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        String[] headers = headerLine.split(",");
        Map<String, Integer> headerIndex = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String normalized =
                    headers[i]
                            .trim()
                            .toLowerCase()
                            .replace("_", "")
                            .replace(" ", "");
            headerIndex.put(normalized, i);
        }

        // ===== REQUIRED (ID OPTIONAL NOW) =====
        if (!headerIndex.containsKey("title")) {
            throw new IllegalArgumentException("CSV must contain title column");
        }

        String line;
        while ((line = reader.readLine()) != null) {

            if (line.isBlank()) continue;

            String[] cols =
                    line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

            // ---------- OLD TASK ID (OPTIONAL) ----------
            Integer oldTaskId = null;
            if (headerIndex.containsKey("id")) {
                String idRaw = cols[headerIndex.get("id")].trim();
                if (!idRaw.isEmpty()) {
                    try {
                        oldTaskId = Integer.parseInt(idRaw);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // ---------- TITLE ----------
            String title =
                    cols[headerIndex.get("title")]
                            .replace("\"", "")
                            .trim();

            if (title.isEmpty()) continue; // skip invalid rows

            // ---------- CREATED AT ----------
            Instant createdAt = Instant.now();
            if (headerIndex.containsKey("createdat")) {
                String createdAtRaw =
                        cols[headerIndex.get("createdat")].trim();
                try {
                    if (!createdAtRaw.isEmpty()) {
                        createdAt = Instant.parse(createdAtRaw);
                    }
                } catch (Exception ignored) {
                }
            }

            // ---------- IS ACTIVE ----------
            boolean isActive = true;
            if (headerIndex.containsKey("isactive")) {
                isActive = Boolean.parseBoolean(
                        cols[headerIndex.get("isactive")].trim()
                );
            }

            Task task = new Task();
            task.setUser(user);
            task.setTitle(title);
            task.setCreatedAt(createdAt);
            task.setIsActive(isActive);

            Task savedTask = taskRepository.save(task);

            // ---------- MAPPING ONLY IF ID EXISTS ----------
            if (oldTaskId != null) {
                taskIdMapping.put(oldTaskId, savedTask.getId());
            }
        }

        return taskIdMapping;
    }

    @Transactional
    public void importTaskCompletionsCsv(
            MultipartFile file,
            Map<Integer, Integer> taskIdMapping
    ) throws IOException {

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(file.getInputStream()));

        String headerLine = reader.readLine();
        if (headerLine == null) return;

        String[] headers = headerLine.split(",");
        Map<String, Integer> headerIndex = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String normalized =
                    headers[i]
                            .trim()
                            .toLowerCase()
                            .replace("_", "")
                            .replace(" ", "");
            headerIndex.put(normalized, i);
        }

        if (!headerIndex.containsKey("taskid")
                || !headerIndex.containsKey("completeddate")) {
            throw new IllegalArgumentException(
                    "CSV must contain taskId and completedDate columns"
            );
        }

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue; // skip empty lines

            String[] cols = line.split(",");

            // Skip if columns are fewer than expected
            if (cols.length < headers.length) continue;

            try {
                Integer oldTaskId = Integer.parseInt(cols[headerIndex.get("taskid")].trim());
                LocalDate completedDate = LocalDate.parse(cols[headerIndex.get("completeddate")].trim());

                Integer newTaskId = taskIdMapping.get(oldTaskId);
                if (newTaskId == null) continue;

                Task task = taskRepository.findById(newTaskId).orElseThrow();

                if (!completionRepository.existsByTaskAndCompletedDate(task, completedDate)) {
                    TaskCompletions tc = new TaskCompletions();
                    tc.setTask(task);
                    tc.setCompletedDate(completedDate);
                    completionRepository.save(tc);
                }
            } catch (Exception e) {
                // skip lines with parsing errors
                continue;
            }
        }

    }



}

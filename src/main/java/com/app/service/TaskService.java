package com.app.service;

import com.app.dto.InsightsResponse;
import com.app.dto.TaskFullStatsResponse;
import com.app.dto.TaskResponse;
import com.app.dto.TaskStatsResponse;
import com.app.enums.PeriodType;
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
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;

import static com.app.enums.PeriodType.*;


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
        return taskRepository.findByUserAndIsActiveTrueOrderByCreatedAtDesc(getCurrentUser())
                .stream()
                .map(task -> new TaskResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getIsActive(),
                        task.getHasSubtasks(),
                        task.getCreatedAt()
                ))
                .toList();
    }

    public Task createTask(String title,Boolean hasSubtasks) {
        Task task = new Task();
        task.setTitle(title);
        task.setUser(getCurrentUser());
        task.setHasSubtasks(hasSubtasks != null && hasSubtasks);
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

    public Map<String, List<LocalDate>> getLast12MonthsDates(Integer taskId) {

        User user = getCurrentUser();

        // Fetch the task
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Map<String, List<LocalDate>> result = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();

        // Loop over last 12 months including current month
        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.from(today).minusMonths(i);
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            List<TaskCompletions> completions = completionRepository.findByTaskAndCompletedDateBetween(
                    task, monthStart, monthEnd
            );

            String monthKey = ym.toString(); // "yyyy-MM"
            List<LocalDate> dates = completions.stream()
                    .map(TaskCompletions::getCompletedDate)
                    .toList();

            result.put(monthKey, dates);
        }

        return result;
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
    public Task editTaskTitle(Integer taskId, String newTitle,Boolean hasSubtasks) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // ✅ Edit title (only if provided)
        if (newTitle != null) {
            if (newTitle.trim().isEmpty()) {
                throw new IllegalArgumentException("Title cannot be empty");
            }
            task.setTitle(newTitle.trim());
        }

        // ✅ Edit hasSubtasks (only if provided)
        if (hasSubtasks != null) {
            task.setHasSubtasks(hasSubtasks);
        }

        return taskRepository.save(task);
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
        writer.println("id,title,created_at,is_active,has_subtasks");

        // CSV Rows
        for (Task task : tasks) {
            writer.printf(
                    "%d,\"%s\",%b,%s%n",
                    task.getId(),
                    task.getTitle().replace("\"", "\"\""),
                    task.getCreatedAt(),
                    task.getIsActive(),
                    task.getHasSubtasks()
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

    public Map<String, Long> getTaskFrequencyPerWeek(Integer taskId, PeriodType periodType, String monthOrYear) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        List<TaskCompletions> completions;
        LocalDate start = null;
        LocalDate end = null;
        LocalDate today = LocalDate.now();

        switch (periodType) {
            case MONTHLY -> {
                YearMonth ym = monthOrYear != null ? YearMonth.parse(monthOrYear) : YearMonth.now();
                start = ym.atDay(1);
                end = ym.atEndOfMonth();
                completions = completionRepository.findByTaskAndCompletedDateBetween(task, start, end);
            }
            case YEARLY -> {
                int year = monthOrYear != null ? Integer.parseInt(monthOrYear) : today.getYear();
                start = LocalDate.of(year, 1, 1);
                end = LocalDate.of(year, 12, 31);
                completions = completionRepository.findByTaskAndCompletedDateBetween(task, start, end);
            }
            case ALL_TIME -> completions = completionRepository.findByTask(task);
            default -> throw new IllegalArgumentException("Unsupported period type");
        }

        // Initialize weekdays in order
        Map<String, Long> frequency = new LinkedHashMap<>();
        frequency.put("SUNDAY", 0L);
        frequency.put("MONDAY", 0L);
        frequency.put("TUESDAY", 0L);
        frequency.put("WEDNESDAY", 0L);
        frequency.put("THURSDAY", 0L);
        frequency.put("FRIDAY", 0L);
        frequency.put("SATURDAY", 0L);

        // Count completions per day
        for (TaskCompletions tc : completions) {
            DayOfWeek day = tc.getCompletedDate().getDayOfWeek();
            frequency.put(day.name(), frequency.get(day.name()) + 1);
        }

        return frequency;
    }

    public TaskFullStatsResponse getTaskFullStats(Integer taskId) {

        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // ---------- COMPLETIONS ----------
        List<TaskCompletions> completions =
                completionRepository.findByTaskOrderByCompletedDateDesc(task);

        // ---------- CURRENT MONTH ----------
        YearMonth currentMonth = YearMonth.now();
        LocalDate cmStart = currentMonth.atDay(1);
        LocalDate cmEnd = currentMonth.atEndOfMonth();

        long completedCurrentMonth =
                completionRepository.countByTaskAndCompletedDateBetween(task, cmStart, cmEnd);

        int totalDaysCurrentMonth = currentMonth.lengthOfMonth();

        double completionPercentageCurrentMonth =
                totalDaysCurrentMonth == 0 ? 0 :
                        (completedCurrentMonth * 100.0) / totalDaysCurrentMonth;

        // ---------- PREVIOUS MONTH ----------
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate pmStart = previousMonth.atDay(1);
        LocalDate pmEnd = previousMonth.atEndOfMonth();

        long completedPreviousMonth =
                completionRepository.countByTaskAndCompletedDateBetween(task, pmStart, pmEnd);

        double monthOverMonthChangePercentage =
                completedPreviousMonth == 0
                        ? 0
                        : ((completedCurrentMonth - completedPreviousMonth) * 100.0)
                        / completedPreviousMonth;

        // ---------- TOTAL COMPLETED ----------
        long totalCompletedDays = completionRepository.countByTask(task);

        // ---------- CURRENT STREAK ----------
        int currentStreak = 0;
        LocalDate currentFrom = null;
        LocalDate currentTo = null;

        LocalDate expectedDate = LocalDate.now();

        for (TaskCompletions tc : completions) {
            if (tc.getCompletedDate().equals(expectedDate)) {
                currentStreak++;
                currentTo = currentTo == null ? expectedDate : currentTo;
                currentFrom = expectedDate;
                expectedDate = expectedDate.minusDays(1);
            } else {
                break;
            }
        }

        // ---------- BEST STREAK (OVERALL) ----------
        int bestStreak = 0;
        int tempStreak = 0;
        LocalDate tempFrom = null;
        LocalDate tempTo = null;

        LocalDate bestFrom = null;
        LocalDate bestTo = null;

        for (int i = 0; i < completions.size(); i++) {

            LocalDate date = completions.get(i).getCompletedDate();

            if (i == 0 || date.equals(completions.get(i - 1).getCompletedDate().minusDays(1))) {
                tempStreak++;
                tempTo = tempTo == null ? date : tempTo;
                tempFrom = date;
            } else {
                tempStreak = 1;
                tempFrom = date;
                tempTo = date;
            }

            if (tempStreak > bestStreak) {
                bestStreak = tempStreak;
                bestFrom = tempFrom;
                bestTo = tempTo;
            }
        }
        Map<String, Long> frequency = getTaskFrequencyPerWeek(taskId, PeriodType.ALL_TIME, null);
        return new TaskFullStatsResponse(
                Math.round(completionPercentageCurrentMonth * 100.0) / 100.0,
                Math.round(monthOverMonthChangePercentage * 100.0) / 100.0,
                totalCompletedDays,
                completedCurrentMonth,
                currentStreak,
                currentFrom,
                currentTo,
                bestStreak,
                bestFrom,
                bestTo,
                frequency
        );
    }

    @Transactional(readOnly = true)
    public InsightsResponse getUserInsights() {

        User user = getCurrentUser();

        List<Task> tasks = taskRepository.findByUserAndIsActiveTrue(user);
        List<TaskCompletions> completions = completionRepository.findByTaskUser(user);

        // 1️⃣ OVERALL COMPLETIONS
        long totalCompletions = completions.size();

        // 2️⃣ HIGHEST TASK COMPLETION
        Map<Task, Long> taskCompletionCount =
                completions.stream()
                        .collect(Collectors.groupingBy(
                                TaskCompletions::getTask,
                                Collectors.counting()
                        ));

        InsightsResponse.HighestTaskCompletion highestTaskCompletion = null;

        if (!taskCompletionCount.isEmpty()) {
            Map.Entry<Task, Long> maxEntry =
                    taskCompletionCount.entrySet()
                            .stream()
                            .max(Map.Entry.comparingByValue())
                            .orElseThrow();

            highestTaskCompletion =
                    new InsightsResponse.HighestTaskCompletion(
                            maxEntry.getKey().getId(),
                            maxEntry.getKey().getTitle(),
                            maxEntry.getValue()
                    );
        }

        // 3️⃣ HIGHEST STREAK (ACROSS ALL TASKS)
        int highestStreak = 0;
        Task highestStreakTask = null;

        for (Task task : tasks) {
            int streak = getTaskBestStreak(task);
            if (streak > highestStreak) {
                highestStreak = streak;
                highestStreakTask = task;
            }
        }

        InsightsResponse.HighestStreak highestStreakDto =
                highestStreakTask == null
                        ? null
                        : new InsightsResponse.HighestStreak(
                        highestStreakTask.getId(),
                        highestStreakTask.getTitle(),
                        highestStreak
                );

        // 4️⃣ PERFECT DAYS (ALL TASKS COMPLETED)
        long perfectDays = calculatePerfectDays(user, tasks);

        // 5️⃣ MEMBER DAYS
        long memberDays =
                ChronoUnit.DAYS.between(
                        user.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        LocalDate.now()
                ) + 1;

        // 6️⃣ CONSISTENCY %
        long totalAvailableDays = 0;
        for (Task task : tasks) {
            LocalDate createdDate =
                    task.getCreatedAt()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

            totalAvailableDays +=
                    ChronoUnit.DAYS.between(createdDate, LocalDate.now()) + 1;
        }

        double consistencyPercentage =
                totalAvailableDays == 0
                        ? 0
                        : Math.round((totalCompletions * 10000.0) / totalAvailableDays) / 100.0;

        return new InsightsResponse(
                totalCompletions,
                highestTaskCompletion,
                highestStreakDto,
                perfectDays,
                memberDays,
                consistencyPercentage
        );
    }

    private int getTaskBestStreak(Task task) {

        List<LocalDate> dates =
                completionRepository.findByTaskOrderByCompletedDateDesc(task)
                        .stream()
                        .map(TaskCompletions::getCompletedDate)
                        .toList();

        int best = 0;
        int current = 0;

        for (int i = 0; i < dates.size(); i++) {
            if (i == 0 || dates.get(i).equals(dates.get(i - 1).minusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
        }

        return best;
    }


    private long calculatePerfectDays(User user, List<Task> tasks) {

        if (tasks.isEmpty()) return 0;

        Map<LocalDate, Long> completionPerDay =
                completionRepository.findByTaskUser(user)
                        .stream()
                        .collect(Collectors.groupingBy(
                                TaskCompletions::getCompletedDate,
                                Collectors.mapping(
                                        tc -> tc.getTask().getId(),
                                        Collectors.toSet()
                                )
                        ))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> (long) e.getValue().size()
                        ));

        long totalTasks = tasks.size();

        return completionPerDay.values()
                .stream()
                .filter(count -> count == totalTasks)
                .count();
    }


}

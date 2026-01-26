package com.app.service;

import com.app.dto.*;
import com.app.enums.PeriodType;
import com.app.model.Task;
import com.app.model.TaskCompletions;
import com.app.model.User;
import com.app.repository.TaskCompletionRepository;
import com.app.repository.TaskRepository;
import com.app.repository.UserRepository;
import com.app.config.SecurityUtil;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class GraphService {

    private final TaskCompletionRepository completionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public GraphService(
            TaskCompletionRepository completionRepository,
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.completionRepository = completionRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email).orElseThrow();
    }

    public List<MonthlyTaskCompletionResponse> getMonthlyCompletionStats(Integer taskId) {

        User user = getCurrentUser();
        Year currentYear = Year.now();

        // Initialize all months with 0
        Map<Month, Long> monthCountMap = new LinkedHashMap<>();
        for (Month month : Month.values()) {
            monthCountMap.put(month, 0L);
        }

        List<TaskCompletions> completions;

        if (taskId != null) {
            // Validate task ownership
            Task task = taskRepository.findById(taskId)
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            completions = completionRepository.findByTaskAndCompletedDateBetween(
                    task,
                    currentYear.atDay(1),
                    currentYear.atMonth(12).atEndOfMonth()
            );
        } else {
            // All tasks of the user
            completions = completionRepository.findByTaskUserAndCompletedDateBetween(
                    user,
                    currentYear.atDay(1),
                    currentYear.atMonth(12).atEndOfMonth()
            );
        }

        // Count completions per month
        for (TaskCompletions tc : completions) {
            Month month = tc.getCompletedDate().getMonth();
            monthCountMap.put(month, monthCountMap.get(month) + 1);
        }

        // Convert to response list
        List<MonthlyTaskCompletionResponse> response = new ArrayList<>();
        for (Month month : monthCountMap.keySet()) {
            response.add(
                    new MonthlyTaskCompletionResponse(
                            month.name(),
                            monthCountMap.get(month)
                    )
            );
        }

        return response;
    }

    public List<TaskCompletionSummaryResponse> getTaskCompletionSummary(PeriodType period) {

        User user = getCurrentUser();
        List<Task> tasks = taskRepository.findByUserAndIsActiveTrue(user);

        final LocalDate startDate;
        final LocalDate endDate = LocalDate.now();

        if (period == PeriodType.MONTHLY) {
            YearMonth ym = YearMonth.now();
            startDate = ym.atDay(1);
        } else if (period == PeriodType.YEARLY) {
            Year year = Year.now();
            startDate = year.atDay(1);
        } else {
            startDate = null; // ALL_TIME
        }

        return tasks.stream()
                .map(task -> {
                    long count = (startDate == null)
                            ? completionRepository.countByTask(task)
                            : completionRepository.countByTaskAndCompletedDateBetween(
                            task, startDate, endDate
                    );

                    return new TaskCompletionSummaryResponse(
                            task.getId(),
                            task.getTitle(),
                            count
                    );
                })
                .toList();
    }

    public List<MonthlyTaskGroupResponse> getMonthlyTaskDetails(Integer taskId) {

        User user = getCurrentUser();

        List<Task> tasks = (taskId == null)
                ? taskRepository.findByUserAndIsActiveTrue(user)
                : List.of(
                taskRepository.findById(taskId)
                        .filter(t -> t.getUser().getId().equals(user.getId()))
                        .orElseThrow(() -> new RuntimeException("Task not found"))
        );

        // 🔹 Use current date
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

// 🔹 Start from 11 months ago (inclusive)
        YearMonth startMonth = currentMonth.minusMonths(11);

// 🔹 Always last 12 months
        long monthsBetween = 12;


        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

        return tasks.stream()
                .flatMap(task ->
                        Stream.iterate(startMonth, ym -> ym.plusMonths(1))
                                .limit(monthsBetween)
                                .map(month -> {

                                    LocalDate start = month.atDay(1);
                                    LocalDate end;

                                    // 🔹 Current month → till today
                                    if (month.equals(currentMonth)) {
                                        end = today;
                                    } else {
                                        end = month.atEndOfMonth();
                                    }

                                    long count = completionRepository
                                            .countByTaskAndCompletedDateBetween(
                                                    task, start, end
                                            );

                                    return new AbstractMap.SimpleEntry<>(
                                            month,
                                            new MonthlyTaskDetailResponse(
                                                    task.getId(),
                                                    task.getTitle(),
                                                    count
                                            )
                                    );
                                })
                )
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,   // 🔹 preserves Jan → Dec order
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.toList()
                        )
                ))
                .entrySet()
                .stream()
                .map(e -> new MonthlyTaskGroupResponse(
                        e.getKey().toString(),        // 2025-01
                        e.getKey().format(formatter), // January 2025
                        e.getValue()
                ))
                .toList();
    }


    public List<TaskPlannedActualResponse> getTaskPlannedActual(Integer taskId) {

        if (taskId == null) {
            throw new IllegalArgumentException("taskId is required");
        }

        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        LocalDate today = LocalDate.now();

        // 🔹 Start from Jan 1st of current year
        YearMonth startMonth = YearMonth.of(today.getYear(), 1);
        YearMonth currentMonth = YearMonth.from(today);

        long monthsBetween =
                ChronoUnit.MONTHS.between(startMonth, currentMonth) + 1;

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

        return Stream.iterate(startMonth, ym -> ym.plusMonths(1))
                .limit(monthsBetween)
                .map(month -> {

                    LocalDate start = month.atDay(1);
                    LocalDate end;

                    // 🔹 For current month, stop at today
                    if (month.equals(currentMonth)) {
                        end = today;
                    } else {
                        end = month.atEndOfMonth();
                    }

                    long plannedDays =
                            ChronoUnit.DAYS.between(start, end) + 1;

                    long actualDays = completionRepository
                            .countByTaskAndCompletedDateBetween(task, start, end);

                    return new TaskPlannedActualResponse(
                            month.toString(),        // monthKey
                            month.format(formatter), // monthLabel
                            plannedDays,
                            actualDays
                    );
                })
                .toList();
    }




}

package com.app.controller;

import com.app.dto.TaskResponse;
import com.app.dto.TaskStatsResponse;
import com.app.model.Task;
import com.app.service.TaskService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@CrossOrigin(origins = "http://localhost:5174")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // list tasks for logged-in user
    @GetMapping
    public List<TaskResponse> getMyTasks() {
        return taskService.getMyTasks();
    }

    // create task
    @PostMapping("/createTask")
    public Task createTask(@RequestBody Map<String, String> body) {
        return taskService.createTask(body.get("title"));
    }

    // mark task completed
    @PostMapping("/{taskId}/complete")
    public void completeTask(@PathVariable Integer taskId) {
        taskService.markCompleted(taskId, LocalDate.now());
    }

    @PostMapping("/{taskId}/toggle")
    public void toggleTask(
            @PathVariable Integer taskId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        taskService.toggleCompleted(taskId, date);
    }

    @GetMapping("/{taskId}/streak")
    public int getTaskStreak(@PathVariable Integer taskId) {
        return taskService.getTaskStreak(taskId);
    }

    @GetMapping("/{taskId}/stats")
    public TaskStatsResponse getTaskStats(@PathVariable Integer taskId) {
        return taskService.getTaskStats(taskId);
    }


    @GetMapping("/completed-dates")
    public Map<Integer, List<LocalDate>> getAllCompletedDates(
            @RequestParam(required = false) Integer taskId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") String month
    ) {
        return taskService.getAllCompletedDates(taskId, month);
    }


    // Delete a task along with its completions
    @DeleteMapping("/{taskId}")
    public Map<String, String> deleteTask(@PathVariable Integer taskId) {
        taskService.deleteTask(taskId);
        return Map.of("message", "Task deleted successfully");
    }

    // Edit task title
    @PutMapping("/{taskId}/edit")
    public Task editTaskTitle(@PathVariable Integer taskId, @RequestBody Map<String, String> body) {
        String newTitle = body.get("title");
        return taskService.editTaskTitle(taskId, newTitle);
    }

    @GetMapping("/export/tasks")
    public void exportTasksCsv(HttpServletResponse response) throws IOException {
        taskService.exportTasksCsv(response);
    }

    @GetMapping("/export/task-completions")
    public void exportTaskCompletionsCsv(HttpServletResponse response) throws IOException {
        taskService.exportTaskCompletionsCsv(response);
    }

    @PostMapping("/import/tasks")
    public Map<Integer, Integer> importTasks(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return taskService.importTasksCsv(file);
    }

    @PostMapping("/import/task-completions")
    public Map<String, String> importTaskCompletions(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<Integer, Integer> taskIdMapping
    ) throws IOException {
        taskService.importTaskCompletionsCsv(file, taskIdMapping);
        return Map.of("message", "Task completions imported successfully");
    }


}

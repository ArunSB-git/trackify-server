package com.app.controller;

import com.app.dto.CreateSubtaskRequest;
import com.app.dto.SubTaskResponse;
import com.app.dto.SubtaskStatusResponse;
import com.app.model.Subtask;
import com.app.service.SubtaskService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5174")
@RestController
@RequestMapping("/api/subtasks")
public class SubtaskController {

    private final SubtaskService subtaskService;

    public SubtaskController(SubtaskService subtaskService) {
        this.subtaskService = subtaskService;
    }

    // Create subtask
    @PostMapping("/create")
    public ResponseEntity<?> createSubtask(
            @RequestBody CreateSubtaskRequest request) {
        try {
            return ResponseEntity.ok(
                    subtaskService.createSubtask(request.taskId(), request.title())
            );
        } catch (RuntimeException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", ex.getMessage()));
        }
    }


    // List subtasks for a task
    @GetMapping("/list/{taskId}")
    public List<SubTaskResponse> getSubtasks(@PathVariable Integer taskId) {
        return subtaskService.getSubtasks(taskId);
    }

    @PutMapping("/{subtaskId}/edit")
    public Subtask editSubtask(@PathVariable Integer subtaskId, @RequestBody Map<String, String> body) {
        String newTitle = body.get("title");
        return subtaskService.editSubtask(subtaskId, newTitle);
    }

    @DeleteMapping("/{subtaskId}")
    public Map<String, String> deleteSubtask(@PathVariable Integer subtaskId) {
        subtaskService.deleteSubtask(subtaskId);
        return Map.of("message", "Subtask deleted successfully");
    }

    @PostMapping("/{subtaskId}/toggle")
    public Map<String, String> toggleSubtask(
            @PathVariable Integer subtaskId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        subtaskService.toggleSubtaskCompleted(subtaskId, date);
        return Map.of("message", "Subtask toggled successfully");
    }

    @GetMapping("/{taskId}/subtasks/status")
    public List<SubtaskStatusResponse> getSubtaskStatus(
            @PathVariable Integer taskId,
            @RequestParam LocalDate date
    ) {
        return subtaskService.getSubtaskStatusByDate(taskId, date);
    }

}

package com.app.controller;

import com.app.dto.MonthlyTaskCompletionResponse;
import com.app.dto.MonthlyTaskGroupResponse;
import com.app.dto.TaskCompletionSummaryResponse;
import com.app.dto.TaskPlannedActualResponse;
import com.app.enums.PeriodType;
import com.app.service.GraphService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5174")
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/monthly")
    public List<MonthlyTaskCompletionResponse> getMonthlyStats(
            @RequestParam(required = false) Integer taskId
    ) {
        return graphService.getMonthlyCompletionStats(taskId);
    }

    @GetMapping("/task-summary")
    public List<TaskCompletionSummaryResponse> getTaskSummary(
            @RequestParam PeriodType period
    ) {
        return graphService.getTaskCompletionSummary(period);
    }

    @GetMapping("/monthly-task-details")
    public List<MonthlyTaskGroupResponse> getMonthlyTaskDetails(
            @RequestParam(required = false) Integer taskId
    ) {
        return graphService.getMonthlyTaskDetails(taskId);
    }

    @GetMapping("/task/planned-actual")
    public List<TaskPlannedActualResponse> getTaskPlannedActual(
            @RequestParam Integer taskId
    ) {
        return graphService.getTaskPlannedActual(taskId);
    }



}
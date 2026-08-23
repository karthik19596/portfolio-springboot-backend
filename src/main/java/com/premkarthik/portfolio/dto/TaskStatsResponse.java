package com.premkarthik.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskStatsResponse {
    private long total;
    private long todo;
    private long inProgress;
    private long done;
    private long lowPriority;
    private long mediumPriority;
    private long highPriority;
    private double completionRate;
}

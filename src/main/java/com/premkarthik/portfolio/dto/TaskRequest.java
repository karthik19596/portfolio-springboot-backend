package com.premkarthik.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be under 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must be under 2000 characters")
    private String description;

    private String status = "TODO";
    private String priority = "MEDIUM";
}

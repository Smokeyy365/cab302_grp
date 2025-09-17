package com.cab302.eduplanner.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain model for a task.
 * Maps to DB table `tasks` with snake_case columns.
 * dueDate stored as TEXT ISO (YYYY-MM-DD) in SQLite.
 */
public class Task {
    private Long taskId;              // tasks.task_id
    private Long userId;              // tasks.user_id

    private String subject;           // tasks.subject
    private String title;             // tasks.title   (required)
    private LocalDate dueDate;        // tasks.due_date
    private String notes;             // tasks.notes

    private Integer weight;           // tasks.weight
    private Double achievedMark;      // tasks.achieved_mark
    private Double maxMark;           // tasks.max_mark

    private LocalDateTime createdAt;  // tasks.created_at TEXT
    private LocalDateTime updatedAt;  // tasks.updated_at TEXT

    public Task() {}

    // New flow constructor
    public Task(Long userId, String subject, String title, LocalDate dueDate,
                String notes, Integer weight, Double achievedMark, Double maxMark) {
        this.userId = userId;
        this.subject = subject;
        this.title = title;
        this.dueDate = dueDate;
        this.notes = notes;
        this.weight = weight;
        this.achievedMark = achievedMark;
        this.maxMark = maxMark;
    }

    // Getters and setters
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Double getAchievedMark() { return achievedMark; }
    public void setAchievedMark(Double achievedMark) { this.achievedMark = achievedMark; }

    public Double getMaxMark() { return maxMark; }
    public void setMaxMark(Double maxMark) { this.maxMark = maxMark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Task{" +
                "taskId=" + taskId +
                ", userId=" + userId +
                ", subject='" + subject + '\'' +
                ", title='" + title + '\'' +
                ", dueDate=" + dueDate +
                ", weight=" + weight +
                ", achievedMark=" + achievedMark +
                ", maxMark=" + maxMark +
                '}';
    }
}

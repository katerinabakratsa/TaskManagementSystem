package com.taskmanagementsystem;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a Task with title, description, category, priority, deadline and status.
 */
public class Task {
    private String id;
    private String title;
    private String description;
    private String categoryId;   // foreign key to Category
    private String priorityId;   // foreign key to Priority
    private LocalDate deadline;
    private TaskStatus status;   // One of {OPEN, IN_PROGRESS, POSTPONED, COMPLETED, DELAYED}

    // Empty constructor for JSON
    public Task() {
    }

    public Task(String title, String description, String categoryId, String priorityId, LocalDate deadline) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.priorityId = priorityId;
        this.deadline = deadline;
        this.status = TaskStatus.OPEN;
    }

    // Getters / Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(String priorityId) {
        this.priorityId = priorityId;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * If the task is not COMPLETED and the deadline is in the past, set it to DELAYED.
     */
    public void checkIfShouldBeDelayed() {
        if (status != TaskStatus.COMPLETED && deadline != null) {
            if (deadline.isBefore(LocalDate.now())) {
                this.status = TaskStatus.DELAYED;
            }
        }
    }
}

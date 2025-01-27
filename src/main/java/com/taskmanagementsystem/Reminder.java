package com.taskmanagementsystem;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a Reminder, which is always linked to a specific Task.
 */
public class Reminder {
    private String id;
    private String taskId;      // foreign key to Task
    private ReminderType type;  // ONE_DAY_BEFORE, ...
    private LocalDate reminderDate;

    // Empty constructor for JSON
    public Reminder() {
    }

    public Reminder(String taskId, ReminderType type, LocalDate reminderDate) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.type = type;
        this.reminderDate = reminderDate;
    }

    // Getters / Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public ReminderType getType() {
        return type;
    }

    public void setType(ReminderType type) {
        this.type = type;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }
}

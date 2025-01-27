package com.taskmanagementsystem;

import java.time.LocalDate;

public class Task {
    private String title;
    private String description;
    private String category;   // π.χ. "Work", "Personal", κ.λπ.
    private String priority;   // π.χ. "Low", "Medium", "High"
    private LocalDate deadline;
    private String status;     // "Open", "In Progress", "Postponed", "Completed", "Delayed"

    public Task() {
        // Απαιτείται κενός constructor για Jackson (JSON)
    }

    public Task(String title, String description, String category, String priority, LocalDate deadline) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.deadline = deadline;
        this.status = "Open"; // Default status
    }

    // -----------------------------------------
    // Getters / Setters
    // -----------------------------------------
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Αν δεν είναι Completed και έχει περάσει η ημερομηνία deadline,
     * ορίζει αυτόματα το status σε "Delayed".
     */
    public void updateStatusIfDelayed() {
        if (!"Completed".equals(this.status) && deadline != null) {
            if (LocalDate.now().isAfter(deadline)) {
                this.status = "Delayed";
            }
        }
    }
}

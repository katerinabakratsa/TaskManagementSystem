package com.taskmanagementsystem;

import java.time.LocalDate;

public class Reminder {
    private Task task;        // Η εργασία για την οποία ορίζεται η υπενθύμιση
    private String type;      // π.χ. "1 day before", "1 week before", "Custom date"
    private LocalDate reminderDate;

    public Reminder() {
        // Απαιτείται κενός constructor για Jackson (JSON)
    }

    public Reminder(Task task, String type, LocalDate reminderDate) {
        this.task = task;
        this.type = type;
        this.reminderDate = reminderDate;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }
}

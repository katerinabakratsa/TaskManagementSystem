package com.taskmanagementsystem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a Task with title, description, category, priority, deadline and status.
 */
public class Task {
    private final String id;
    private final StringProperty title;
    private final StringProperty description;
    private final StringProperty categoryId;   // observable property για Category ID
    private final StringProperty priorityId;   // observable property για Priority ID
    private final ObjectProperty<LocalDate> deadline;
    private final ObjectProperty<TaskStatus> status; // observable property για Status

    // Empty constructor for JSON
    public Task() {
        this.id = UUID.randomUUID().toString();
        this.title = new SimpleStringProperty("");
        this.description = new SimpleStringProperty("");
        this.categoryId = new SimpleStringProperty();
        this.priorityId = new SimpleStringProperty();
        this.deadline = new SimpleObjectProperty<>();
        this.status = new SimpleObjectProperty<>(TaskStatus.OPEN);
    }

    public Task(String title, String description, String categoryId, String priorityId, LocalDate deadline) {
        this.id = UUID.randomUUID().toString();
        this.title = new SimpleStringProperty(title);
        this.description = new SimpleStringProperty(description);
        this.categoryId = new SimpleStringProperty(categoryId);
        this.priorityId = new SimpleStringProperty(priorityId);
        this.deadline = new SimpleObjectProperty<>(deadline);
        this.status = new SimpleObjectProperty<>(TaskStatus.OPEN);
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getCategoryId() {
        return categoryId.get();
    }

    public void setCategoryId(String categoryId) {
        this.categoryId.set(categoryId);
    }

    public StringProperty categoryIdProperty() {
        return categoryId;
    }

    public String getPriorityId() {
        return priorityId.get();
    }

    public void setPriorityId(String priorityId) {
        this.priorityId.set(priorityId);
    }

    public StringProperty priorityIdProperty() {
        return priorityId;
    }

    public LocalDate getDeadline() {
        return deadline.get();
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline.set(deadline);
    }

    public ObjectProperty<LocalDate> deadlineProperty() {
        return deadline;
    }

    public TaskStatus getStatus() {
        return status.get();
    }

    public void setStatus(TaskStatus status) {
        this.status.set(status);
    }

    public ObjectProperty<TaskStatus> statusProperty() {
        return status;
    }

    /**
     * Ελέγχει εάν το task πρέπει να είναι DELAYED (deadline έχει παρέλθει και δεν έχει ολοκληρωθεί)
     */
    public void checkIfShouldBeDelayed() {
        if (!getStatus().equals(TaskStatus.COMPLETED) && getDeadline() != null) {
            if (getDeadline().isBefore(LocalDate.now())) {
                setStatus(TaskStatus.DELAYED);
            }
        }
    }
}

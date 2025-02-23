package com.taskmanagementsystem;

import javafx.beans.property.SimpleStringProperty;
import java.util.UUID;

/**
 * Represents a Category (e.g., "Work", "Personal").
 */
public class Category {
    private String id;
    private SimpleStringProperty name;

    // Empty constructor for JSON
    public Category() {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty("");
    }

    public Category(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty(name);
    }

    public Category(String id, String name) {
        this.id = id;
        this.name = new SimpleStringProperty(name);
    }

    // Getters / Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    // Μέθοδος για binding στο UI
    public SimpleStringProperty nameProperty() {
        return name;
    }
}

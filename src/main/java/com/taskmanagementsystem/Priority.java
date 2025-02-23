package com.taskmanagementsystem;

import javafx.beans.property.SimpleStringProperty;
import java.util.UUID;

/**
 * Represents a Priority level (e.g., "Default", "High", "Low").
 */
public class Priority {
    private String id;
    private SimpleStringProperty name;

    // Empty constructor for JSON
    public Priority() {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty("");
    }

    public Priority(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = new SimpleStringProperty(name);
    }

    public Priority(String id, String name) {
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

    public SimpleStringProperty nameProperty() {
        return name;
    }
}

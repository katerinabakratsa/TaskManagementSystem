package com.taskmanagementsystem;

import java.util.UUID;

/**
 * Represents a Category (e.g., "Work", "Personal").
 */
public class Category {
    private String id;
    private String name;

    // Empty constructor for JSON
    public Category() {
    }

    public Category(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters / Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

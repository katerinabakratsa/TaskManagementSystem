package com.taskmanagementsystem;

public class Priority {
    private String name; // Όνομα προτεραιότητας (π.χ. "Low", "Medium", "High")

    public Priority(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

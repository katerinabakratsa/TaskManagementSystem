package com.taskmanagementsystem;

import java.util.ArrayList;
import java.util.List;

public class PriorityManager {
    private List<Priority> priorities;

    public PriorityManager() {
        this.priorities = new ArrayList<>();
        // Προκαθορισμένο επίπεδο προτεραιότητας "Default"
        priorities.add(new Priority("Default"));
    }

    // Προσθήκη προτεραιότητας
    public void addPriority(Priority priority) {
        priorities.add(priority);
    }

    // Διαγραφή προτεραιότητας
    public void deletePriority(String name) {
        if (!name.equals("Default")) {
            priorities.removeIf(priority -> priority.getName().equals(name));
        }
    }

    // Επιστροφή όλων των προτεραιοτήτων
    public List<Priority> getAllPriorities() {
        return priorities;
    }
}

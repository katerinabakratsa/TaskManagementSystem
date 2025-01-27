package com.taskmanagementsystem;

import java.util.List;
import java.util.stream.Collectors;

public class SearchManager {
    private TaskManager taskManager;

    public SearchManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    // Αναζήτηση εργασιών
    public List<Task> searchTasks(String title, String category, String priority) {
        return taskManager.getTasksByCategory(category).stream()
                .filter(task -> (title == null || task.getTitle().contains(title)) &&
                        (priority == null || task.getPriority().equals(priority)))
                .collect(Collectors.toList());
    }
}

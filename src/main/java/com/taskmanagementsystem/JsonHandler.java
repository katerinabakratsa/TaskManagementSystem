package com.taskmanagementsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonHandler {
    private static final String TASK_FILE_PATH = "medialab/tasks.json";
    private static final String CATEGORY_FILE_PATH = "medialab/categories.json";
    private static final String PRIORITY_FILE_PATH = "medialab/priorities.json";
    private static final String REMINDER_FILE_PATH = "medialab/reminders.json";

    public static void saveTasks(List<Task> tasks) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(TASK_FILE_PATH), tasks);
    }

    public static List<Task> loadTasks() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(TASK_FILE_PATH);
        if (file.exists()) {
            return List.of(mapper.readValue(file, Task[].class));
        }
        return List.of();
    }

    public static void saveCategories(List<String> categories) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(CATEGORY_FILE_PATH), categories);
    }

    public static void savePriorities(List<Priority> priorities) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(PRIORITY_FILE_PATH), priorities);
    }

    public static void saveReminders(List<Reminder> reminders) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(REMINDER_FILE_PATH), reminders);
    }
}

package com.taskmanagementsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Κεντρική κλάση διαχείρισης που αποθηκεύει Tasks + Reminders,
 * και διαχειρίζεται την ανάγνωση/εγγραφή σε JSON αρχείο.
 */
public class TaskManager {

    private List<Task> tasks;
    private List<Reminder> reminders;

    public TaskManager() {
        this.tasks = new ArrayList<>();
        this.reminders = new ArrayList<>();
    }

    // ----------------------------------------------------------
    // Βασικές λειτουργίες Task
    // ----------------------------------------------------------

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void deleteTask(String title) {
        // Διαγράφουμε το Task με βάση το title
        tasks.removeIf(task -> task.getTitle().equals(title));
        // Διαγράφουμε και τις reminders που σχετίζονται
        reminders.removeIf(reminder -> reminder.getTask().getTitle().equals(title));
    }

    public void updateTask(String oldTitle, Task updatedTask) {
        for (int i = 0; i < tasks.size(); i++) {
            Task existing = tasks.get(i);
            if (existing.getTitle().equals(oldTitle)) {
                // Ενημερώνουμε όλα τα πεδία
                existing.setTitle(updatedTask.getTitle());
                existing.setDescription(updatedTask.getDescription());
                existing.setCategory(updatedTask.getCategory());
                existing.setPriority(updatedTask.getPriority());
                existing.setDeadline(updatedTask.getDeadline());
                existing.setStatus(updatedTask.getStatus());
            }
        }
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    public List<Task> getTasksByCategory(String category) {
        return tasks.stream()
                .filter(t -> t.getCategory() != null && t.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // Αναζήτηση
    // ----------------------------------------------------------

    /**
     * Αναζήτηση εργασιών με βάση τίτλο, κατηγορία και προτεραιότητα.
     * Εάν κάποια παράμετρος είναι κενή ή null, παραλείπεται από το φιλτράρισμα.
     */
    public List<Task> searchTasks(String title, String category, String priority) {
        return tasks.stream()
                .filter(task -> {
                    boolean matchTitle = true;
                    boolean matchCategory = true;
                    boolean matchPriority = true;

                    if (title != null && !title.isEmpty()) {
                        matchTitle = task.getTitle() != null && task.getTitle().contains(title);
                    }
                    if (category != null && !category.isEmpty()) {
                        matchCategory = category.equals(task.getCategory());
                    }
                    if (priority != null && !priority.isEmpty()) {
                        matchPriority = priority.equals(task.getPriority());
                    }
                    return matchTitle && matchCategory && matchPriority;
                })
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // Λειτουργίες Reminder
    // ----------------------------------------------------------

    public void addReminder(Task task, String type, String dateString) {
        LocalDate reminderDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Reminder reminder = new Reminder(task, type, reminderDate);
        reminders.add(reminder);
    }

    public void deleteRemindersForTask(Task task) {
        reminders.removeIf(r -> r.getTask().equals(task));
    }

    public List<Reminder> getAllReminders() {
        return reminders;
    }

    public void deleteRemindersForCompletedTasks() {
        reminders.removeIf(r -> "Completed".equals(r.getTask().getStatus()));
    }

    public List<Reminder> getRemindersForTask(Task task) {
        return reminders.stream()
                .filter(r -> r.getTask().equals(task))
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------
    // JSON Save/Load
    // ----------------------------------------------------------

    public void saveDataToJSON(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        // Όμορφη μορφοποίηση
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            DataWrapper data = new DataWrapper(tasks, reminders);
            mapper.writeValue(new File(filePath), data);
            System.out.println("Data saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public void loadDataFromJSON(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath);

        // Αν δεν υπάρχει το αρχείο, ξεκινάμε απλά με άδεια λίστα
        if (!file.exists()) {
            return;
        }

        try {
            DataWrapper data = mapper.readValue(file, DataWrapper.class);
            this.tasks = data.getTasks();
            this.reminders = data.getReminders();
            System.out.println("Data loaded from " + filePath);
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
        }
    }

    /**
     * Ελέγχει όλα τα tasks και αν κάποιο έχει ξεπερασμένο deadline (και δεν είναι Completed),
     * το μετατρέπει σε "Delayed".
     */
    public void checkAndUpdateDelayedTasks() {
        for (Task task : tasks) {
            task.updateStatusIfDelayed();
        }
    }

    // ----------------------------------------------------------
    // Εσωτερική κλάση για JSON Serialization
    // ----------------------------------------------------------
    private static class DataWrapper {
        private List<Task> tasks;
        private List<Reminder> reminders;

        public DataWrapper() {
            // κενός constructor για Jackson
        }

        public DataWrapper(List<Task> tasks, List<Reminder> reminders) {
            this.tasks = tasks;
            this.reminders = reminders;
        }

        public List<Task> getTasks() {
            return tasks;
        }

        public void setTasks(List<Task> tasks) {
            this.tasks = tasks;
        }

        public List<Reminder> getReminders() {
            return reminders;
        }

        public void setReminders(List<Reminder> reminders) {
            this.reminders = reminders;
        }
    }
}

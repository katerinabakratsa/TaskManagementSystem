package com.taskmanagementsystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Central class that manages all categories, priorities, tasks, and reminders.
 *
 * Provides methods to load/save JSON files, create/update/delete objects,
 * and handle rules like:
 *  - Deleting tasks when a category is removed
 *  - Reassigning "Default" priority if a priority is removed
 *  - Checking and updating "Delayed" tasks if their deadline is passed
 */
public class DataManager {
    private static final String MEDIALAB_FOLDER = "medialab";
    private static final String CATEGORIES_FILE = MEDIALAB_FOLDER + "/categories.json";
    private static final String PRIORITIES_FILE = MEDIALAB_FOLDER + "/priorities.json";
    private static final String TASKS_FILE = MEDIALAB_FOLDER + "/tasks.json";
    private static final String REMINDERS_FILE = MEDIALAB_FOLDER + "/reminders.json";

    // Χρησιμοποιούμε ObservableList αντί για απλό List
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<Priority> priorities = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<Reminder> reminders = FXCollections.observableArrayList();

    // We'll store the ID of the "Default" priority for easy reference
    private String defaultPriorityId;

    /**
     * Constructor
     */
    public DataManager() {
        // Αρχικοποίηση των λιστών γίνεται στο loadAllData
    }

    // ---------------------------------------------------------------
    // Load / Save JSON
    // ---------------------------------------------------------------

    /**
     * Loads all data (categories, priorities, tasks, reminders) from JSON files in the "medialab" folder.
     * If a file does not exist, it starts with an empty list for that file.
     */
    public void loadAllData() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        File catFile = new File(CATEGORIES_FILE);
        File prioFile = new File(PRIORITIES_FILE);
        File taskFile = new File(TASKS_FILE);
        File remFile = new File(REMINDERS_FILE);

        try {
            // Φορτώνουμε προσωρινά σε απλές λίστες
            List<Category> loadedCategories = new ArrayList<>();
            List<Priority> loadedPriorities = new ArrayList<>();
            List<Task> loadedTasks = new ArrayList<>();
            List<Reminder> loadedReminders = new ArrayList<>();

            if (catFile.exists()) {
                loadedCategories = mapper.readValue(catFile, new TypeReference<>() {});
            }
            if (prioFile.exists()) {
                loadedPriorities = mapper.readValue(prioFile, new TypeReference<>() {});
            }
            if (taskFile.exists()) {
                loadedTasks = mapper.readValue(taskFile, new TypeReference<>() {});
            }
            if (remFile.exists()) {
                loadedReminders = mapper.readValue(remFile, new TypeReference<>() {});
            }

            // Μεταφέρουμε τα δεδομένα στις ObservableLists
            categories.setAll(loadedCategories);
            priorities.setAll(loadedPriorities);
            tasks.setAll(loadedTasks);
            reminders.setAll(loadedReminders);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Αν δεν υπάρχει Default priority, δημιουργείται
        ensureDefaultPriorityExists();
        // Ενημέρωση τυχόν delayed tasks
        updateDelayedTasks();
    }

    /**
     * Saves all data (categories, priorities, tasks, reminders) into separate JSON files in "medialab" folder.
     */
    public void saveAllData() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            File folder = new File(MEDIALAB_FOLDER);
            if (!folder.exists()) {
                folder.mkdir();
            }

            mapper.writeValue(new File(CATEGORIES_FILE), new ArrayList<>(categories));
            mapper.writeValue(new File(PRIORITIES_FILE), new ArrayList<>(priorities));
            mapper.writeValue(new File(TASKS_FILE), new ArrayList<>(tasks));
            mapper.writeValue(new File(REMINDERS_FILE), new ArrayList<>(reminders));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensures that a "Default" priority exists. If not found, creates it.
     */
    private void ensureDefaultPriorityExists() {
        Optional<Priority> defaultP = priorities.stream()
                .filter(p -> p.getName().equalsIgnoreCase("Default"))
                .findFirst();

        if (defaultP.isEmpty()) {
            Priority def = new Priority("Default");
            priorities.add(def);
            defaultPriorityId = def.getId();
        } else {
            defaultPriorityId = defaultP.get().getId();
        }
    }

    /**
     * At initialization, update tasks that should be "Delayed" if their deadline is past.
     */
    private void updateDelayedTasks() {
        for (Task t : tasks) {
            t.checkIfShouldBeDelayed();
        }
    }

    // ---------------------------------------------------------------
    // Category Management
    // ---------------------------------------------------------------
    public List<Category> getAllCategories() {
        return categories;
    }

    public ObservableList<Category> getObservableCategories() {
        return categories;
    }

    public Category createCategory(String name) {
        Category cat = new Category(name);
        categories.add(cat);
        return cat;
    }

    public void renameCategory(Category category, String newName) {
        category.setName(newName);
    }

    public void deleteCategory(Category category) {
        // Διαγράφουμε όλες τις tasks που ανήκουν σε αυτήν
        List<String> taskIdsToRemove = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getCategoryId() != null && t.getCategoryId().equals(category.getId())) {
                taskIdsToRemove.add(t.getId());
            }
        }
        // Αφαιρούμε υπενθυμίσεις για όσες tasks διαγράφηκαν
        reminders.removeIf(r -> taskIdsToRemove.contains(r.getTaskId()));
        // Αφαιρούμε τις tasks
        tasks.removeIf(t -> taskIdsToRemove.contains(t.getId()));

        // Τέλος αφαιρούμε την κατηγορία
        categories.remove(category);
    }

    // ---------------------------------------------------------------
    // Priority Management
    // ---------------------------------------------------------------
    public List<Priority> getAllPriorities() {
        return priorities;
    }

    public ObservableList<Priority> getObservablePriorities() {
        return priorities;
    }

    public Priority createPriority(String name) {
        Priority p = new Priority(name);
        priorities.add(p);
        return p;
    }

    public void renamePriority(Priority priority, String newName) {
        // Αν είναι η Default, αγνοείται
        Priority def = getDefaultPriority();
        if (priority.getId().equals(def.getId())) {
            return;
        }
        priority.setName(newName);
    }

    public void deletePriority(Priority priority) {
        // Αν είναι η default, δεν διαγράφεται
        Priority def = getDefaultPriority();
        if (priority.getId().equals(def.getId())) {
            return;
        }

        // Όσες tasks είχαν αυτό το priority, παίρνουν το Default
        for (Task t : tasks) {
            if (t.getPriorityId().equals(priority.getId())) {
                t.setPriorityId(def.getId());
            }
        }
        priorities.remove(priority);
    }

    public Priority getDefaultPriority() {
        return priorities.stream()
                .filter(p -> p.getId().equals(defaultPriorityId))
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------
    // Task Management
    // ---------------------------------------------------------------
    public List<Task> getAllTasks() {
        return tasks;
    }

    public ObservableList<Task> getObservableTasks() {
        return tasks;
    }

    public Task createTask(String title, String description,
                           Category category, Priority priority,
                           LocalDate deadline) {
        String categoryId = (category != null) ? category.getId() : null;
        String priorityId = (priority != null) ? priority.getId() : getDefaultPriority().getId();

        Task task = new Task(title, description, categoryId, priorityId, deadline);
        tasks.add(task);
        return task;
    }

    public void updateTask(Task task, String newTitle, String newDesc,
                           Category newCategory, Priority newPriority,
                           LocalDate newDeadline, TaskStatus newStatus) {

        TaskStatus previousStatus = task.getStatus();

        task.setTitle(newTitle);
        task.setDescription(newDesc);
        task.setCategoryId((newCategory != null) ? newCategory.getId() : null);
        task.setPriorityId((newPriority != null) ? newPriority.getId() : getDefaultPriority().getId());
        task.setDeadline(newDeadline);
        task.setStatus(newStatus);

        // Αν από άλλη κατάσταση πέρασε σε COMPLETED, διαγράφουμε τις υπενθυμίσεις
        if (previousStatus != TaskStatus.COMPLETED && newStatus == TaskStatus.COMPLETED) {
            reminders.removeIf(r -> r.getTaskId().equals(task.getId()));
            System.out.println("✅ All reminders for task '" + task.getTitle() + "' have been deleted.");
        }
    }

    public void deleteTask(Task task) {
        // Αφαιρούμε όλες τις reminders που ανήκουν σε αυτήν
        reminders.removeIf(r -> r.getTaskId().equals(task.getId()));
        tasks.remove(task);
    }

    // ---------------------------------------------------------------
    // Reminders
    // ---------------------------------------------------------------
    public List<Reminder> getAllReminders() {
        return reminders;
    }

    public ObservableList<Reminder> getObservableReminders() {
        return reminders;
    }

    public Reminder createReminder(Task task, ReminderType type, LocalDate customDate) {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot create reminder for a Completed task.");
        }
        if (type == ReminderType.SPECIFIC_DATE && customDate == null) {
            throw new IllegalArgumentException("Reminder date cannot be empty for SPECIFIC_DATE.");
        }

        LocalDate deadline = task.getDeadline();
        if (deadline == null && type != ReminderType.SPECIFIC_DATE) {
            throw new IllegalArgumentException("Task has no deadline, cannot create this type of reminder.");
        }

        LocalDate reminderDate;
        switch (type) {
            case ONE_DAY_BEFORE:
                reminderDate = deadline.minusDays(1);
                break;
            case ONE_WEEK_BEFORE:
                reminderDate = deadline.minusWeeks(1);
                break;
            case ONE_MONTH_BEFORE:
                reminderDate = deadline.minusMonths(1);
                break;
            case SPECIFIC_DATE:
            default:
                reminderDate = customDate;
        }

        if (reminderDate != null && reminderDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reminder date cannot be in the past.");
        }

        Reminder reminder = new Reminder(task.getId(), type, reminderDate);
        reminders.add(reminder);
        return reminder;
    }

    public void deleteReminder(Reminder reminder) {
        reminders.remove(reminder);
    }

    public void updateReminder(Reminder reminder,
                               Task newTask,
                               ReminderType newType,
                               LocalDate newDate) {
        if (reminder == null) return;

        if (newType == ReminderType.SPECIFIC_DATE && newDate == null) {
            throw new IllegalArgumentException("Reminder date cannot be empty for SPECIFIC_DATE.");
        }

        LocalDate deadline = newTask.getDeadline();
        if (deadline == null && newType != ReminderType.SPECIFIC_DATE) {
            throw new IllegalArgumentException("Task has no deadline, cannot set this type of reminder.");
        }

        // Υπολογίζουμε το σωστό reminderDate
        LocalDate reminderDate;
        switch (newType) {
            case ONE_DAY_BEFORE:
                reminderDate = deadline.minusDays(1);
                break;
            case ONE_WEEK_BEFORE:
                reminderDate = deadline.minusWeeks(1);
                break;
            case ONE_MONTH_BEFORE:
                reminderDate = deadline.minusMonths(1);
                break;
            case SPECIFIC_DATE:
            default:
                reminderDate = newDate;
        }

        if (reminderDate != null && reminderDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reminder date cannot be in the past.");
        }

        reminder.setTaskId(newTask.getId());
        reminder.setType(newType);
        reminder.setReminderDate(reminderDate);

        // Ενημερώνουμε τη λίστα με το τροποποιημένο αντικείμενο
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId().equals(reminder.getId())) {
                reminders.set(i, reminder);
                break;
            }
        }
    }

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------
    public List<Task> searchTasks(String title, Category category, Priority priority) {
        return tasks.stream().filter(task -> {
            boolean matchTitle = true;
            boolean matchCategory = true;
            boolean matchPriority = true;

            if (title != null && !title.isEmpty()) {
                matchTitle = task.getTitle() != null && task.getTitle().toLowerCase().contains(title.toLowerCase());
            }
            if (category != null) {
                matchCategory = category.getId().equals(task.getCategoryId());
            }
            if (priority != null) {
                matchPriority = priority.getId().equals(task.getPriorityId());
            }
            return matchTitle && matchCategory && matchPriority;
        }).toList();
    }

    // ---------------------------------------------------------------
    // Helper lookups
    // ---------------------------------------------------------------
    public Category findCategoryById(String categoryId) {
        if (categoryId == null) return null;
        return categories.stream().filter(c -> c.getId().equals(categoryId)).findFirst().orElse(null);
    }

    public Priority findPriorityById(String priorityId) {
        if (priorityId == null) return null;
        return priorities.stream().filter(p -> p.getId().equals(priorityId)).findFirst().orElse(null);
    }

    public Task getTaskById(String taskId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }
}

package com.taskmanagementsystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Central class that manages all categories, priorities, tasks, and reminders.
 * <p>
 * Provides methods to load/save JSON files, create/update/delete objects, and handle rules like:
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

    private List<Category> categories;
    private List<Priority> priorities;
    private List<Task> tasks;
    private List<Reminder> reminders;

    // We'll store the ID of the "Default" priority for easy reference.
    private String defaultPriorityId;

    /**
     * Constructor initializes empty lists.
     */
    public DataManager() {
        categories = new ArrayList<>();
        priorities = new ArrayList<>();
        tasks = new ArrayList<>();
        reminders = new ArrayList<>();
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
        File catFile = new File(CATEGORIES_FILE);
        File prioFile = new File(PRIORITIES_FILE);
        File taskFile = new File(TASKS_FILE);
        File remFile = new File(REMINDERS_FILE);

        try {
            if (catFile.exists()) {
                categories = mapper.readValue(catFile, new TypeReference<List<Category>>() {});
            }
            if (prioFile.exists()) {
                priorities = mapper.readValue(prioFile, new TypeReference<List<Priority>>() {});
            }
            if (taskFile.exists()) {
                tasks = mapper.readValue(taskFile, new TypeReference<List<Task>>() {});
            }
            if (remFile.exists()) {
                reminders = mapper.readValue(remFile, new TypeReference<List<Reminder>>() {});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If no priorities exist or no "Default" priority found, create it.
        ensureDefaultPriorityExists();
        // Check delayed tasks
        updateDelayedTasks();
    }

    /**
     * Saves all data (categories, priorities, tasks, reminders) into separate JSON files in "medialab" folder.
     */
    public void saveAllData() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Create folder "medialab" if it doesn't exist
            File folder = new File(MEDIALAB_FOLDER);
            if (!folder.exists()) {
                folder.mkdir();
            }

            mapper.writeValue(new File(CATEGORIES_FILE), categories);
            mapper.writeValue(new File(PRIORITIES_FILE), priorities);
            mapper.writeValue(new File(TASKS_FILE), tasks);
            mapper.writeValue(new File(REMINDERS_FILE), reminders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ensures that a "Default" priority exists. If not found, creates it.
     */
    private void ensureDefaultPriorityExists() {
        // If user has not created "Default", let's ensure it is there
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

    /**
     * Returns all categories.
     *
     * @return a list of Category objects
     */
    public List<Category> getAllCategories() {
        return categories;
    }

    /**
     * Creates a new Category with the given name.
     *
     * @param name the category name
     * @return the newly created Category
     */
    public Category createCategory(String name) {
        Category cat = new Category(name);
        categories.add(cat);
        return cat;
    }

    /**
     * Updates (renames) a category by setting a new name.
     * @param category the Category object to rename
     * @param newName the new name
     */
    public void renameCategory(Category category, String newName) {
        category.setName(newName);
    }

    /**
     * Deletes the specified category and removes all tasks that belong to it,
     * along with the reminders for those tasks.
     * @param category the category to delete
     */
    public void deleteCategory(Category category) {
        // Remove tasks that have this category
        List<String> taskIdsToRemove = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getCategoryId() != null && t.getCategoryId().equals(category.getId())) {
                taskIdsToRemove.add(t.getId());
            }
        }
        // Remove reminders associated with those tasks
        reminders.removeIf(r -> taskIdsToRemove.contains(r.getTaskId()));
        // Remove the tasks
        tasks.removeIf(t -> taskIdsToRemove.contains(t.getId()));
        // Finally remove the category
        categories.remove(category);
    }

    // ---------------------------------------------------------------
    // Priority Management
    // ---------------------------------------------------------------

    /**
     * Returns the list of all priorities.
     */
    public List<Priority> getAllPriorities() {
        return priorities;
    }

    /**
     * Creates a new Priority (except if name is "Default" we typically already have one).
     * @param name the name
     * @return the new Priority
     */
    public Priority createPriority(String name) {
        Priority p = new Priority(name);
        priorities.add(p);
        return p;
    }

    /**
     * Renames a Priority. If it's the "Default" priority, does nothing (or you could throw an exception).
     * @param priority the priority to rename
     * @param newName  the new name
     */
    public void renamePriority(Priority priority, String newName) {
        // If it's the default priority, ignore or show error
        Priority defaultP = getDefaultPriority();
        if (priority.getId().equals(defaultP.getId())) {
            // do nothing or throw exception
            return;
        }
        priority.setName(newName);
    }

    /**
     * Deletes the given Priority, reassigning all tasks that use it to the Default priority.
     * @param priority the priority to delete
     */
    public void deletePriority(Priority priority) {
        // If it's the default, do not delete
        Priority def = getDefaultPriority();
        if (priority.getId().equals(def.getId())) {
            return;
        }

        // Reassign tasks that used this priority to default
        for (Task t : tasks) {
            if (t.getPriorityId().equals(priority.getId())) {
                t.setPriorityId(def.getId());
            }
        }
        priorities.remove(priority);
    }

    /**
     * Returns the Priority object that is "Default".
     */
    public Priority getDefaultPriority() {
        return priorities.stream()
                .filter(p -> p.getId().equals(defaultPriorityId))
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------
    // Task Management
    // ---------------------------------------------------------------

    /**
     * Returns all tasks.
     */
    public List<Task> getAllTasks() {
        return tasks;
    }

    /**
     * Creates a new Task. Default status is OPEN.
     */
    public Task createTask(String title, String description, Category category, Priority priority, LocalDate deadline) {
        String categoryId = category != null ? category.getId() : null;
        String priorityId = priority != null ? priority.getId() : getDefaultPriority().getId();

        Task task = new Task(title, description, categoryId, priorityId, deadline);
        tasks.add(task);
        return task;
    }

    /**
     * Updates all fields of a Task (title, desc, category, priority, deadline, status).
     *
     * @param task         The Task to update
     * @param newTitle     new title
     * @param newDesc      new description
     * @param newCategory  new category (can be null if none)
     * @param newPriority  new priority (if null, use Default)
     * @param newDeadline  new deadline
     * @param newStatus    new status
     */
    public void updateTask(Task task, String newTitle, String newDesc,
                           Category newCategory, Priority newPriority,
                           LocalDate newDeadline, TaskStatus newStatus) {

        task.setTitle(newTitle);
        task.setDescription(newDesc);
        task.setCategoryId(newCategory != null ? newCategory.getId() : null);
        task.setPriorityId(newPriority != null ? newPriority.getId() : getDefaultPriority().getId());
        task.setDeadline(newDeadline);
        task.setStatus(newStatus);

        // If status -> COMPLETED, remove all reminders for that task
        if (newStatus == TaskStatus.COMPLETED) {
            reminders.removeIf(r -> r.getTaskId().equals(task.getId()));
        }
    }

    /**
     * Deletes the given task. Also removes all related reminders.
     */
    public void deleteTask(Task task) {
        // Remove reminders
        reminders.removeIf(r -> r.getTaskId().equals(task.getId()));
        tasks.remove(task);
    }

    // ---------------------------------------------------------------
    // Reminders
    // ---------------------------------------------------------------

    /**
     * Returns all reminders currently defined.
     */
    public List<Reminder> getAllReminders() {
        return reminders;
    }

    /**
     * Creates a reminder for a specific task, given a type (ONE_DAY_BEFORE etc).
     * If the task is COMPLETED, we should not allow a new reminder (throw an exception or ignore).
     * Also checks if the resulting reminderDate is valid (not before today's date).
     */
    public Reminder createReminder(Task task, ReminderType type, LocalDate customDate) {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot create reminder for a Completed task.");
        }
        // Calculate the reminderDate if type != SPECIFIC_DATE
        LocalDate reminderDate;
        LocalDate deadline = task.getDeadline();
        if (deadline == null) {
            throw new IllegalArgumentException("Task has no deadline, cannot create this type of reminder.");
        }

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
                if (customDate == null) {
                    throw new IllegalArgumentException("Custom date is null for SPECIFIC_DATE type.");
                }
                reminderDate = customDate;
        }

        // Check if the reminder date is before "today" in a meaningless way
        if (reminderDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reminder date has already passed or invalid.");
        }

        Reminder reminder = new Reminder(task.getId(), type, reminderDate);
        reminders.add(reminder);
        return reminder;
    }

    /**
     * Deletes a specific reminder.
     */
    public void deleteReminder(Reminder reminder) {
        reminders.remove(reminder);
    }

    /**
     * Searches for reminders related to a specific Task.
     */
    public List<Reminder> getRemindersByTask(Task task) {
        List<Reminder> result = new ArrayList<>();
        for (Reminder r : reminders) {
            if (r.getTaskId().equals(task.getId())) {
                result.add(r);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------

    /**
     * Searches tasks by an optional title substring, category, priority.
     *
     * @param title     partial title to match (or null/empty)
     * @param category  category to match (or null if not used)
     * @param priority  priority to match (or null if not used)
     * @return matching tasks
     */
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

    /**
     * Finds a Category by ID.
     */
    public Category findCategoryById(String categoryId) {
        if (categoryId == null) return null;
        return categories.stream().filter(c -> c.getId().equals(categoryId)).findFirst().orElse(null);
    }

    /**
     * Finds a Priority by ID.
     */
    public Priority findPriorityById(String priorityId) {
        if (priorityId == null) return null;
        return priorities.stream().filter(p -> p.getId().equals(priorityId)).findFirst().orElse(null);
    }
}

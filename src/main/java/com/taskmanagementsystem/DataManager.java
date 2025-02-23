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

    // Using ObservableList for live updates in the UI
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<Priority> priorities = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<Reminder> reminders = FXCollections.observableArrayList();

    // We'll store the ID of the "Default" priority for easy reference
    private String defaultPriorityId;

    /**
     * Default constructor.
     * Lists are loaded via loadAllData().
     */
    public DataManager() {
    }

    // ---------------------------------------------------------------
    // Load / Save JSON
    // ---------------------------------------------------------------

    /**
     * Loads all data (categories, priorities, tasks, reminders)
     * from JSON files in the "medialab" folder.
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

            categories.setAll(loadedCategories);
            priorities.setAll(loadedPriorities);
            tasks.setAll(loadedTasks);
            reminders.setAll(loadedReminders);

        } catch (IOException e) {
            e.printStackTrace();
        }

        ensureDefaultPriorityExists();
        updateDelayedTasks();
    }

    /**
     * Saves all data (categories, priorities, tasks, reminders)
     * into separate JSON files in the "medialab" folder.
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

    // ---------------------------------------------------------------
    // Category Management
    // ---------------------------------------------------------------

    /**
     * Returns a List of all categories currently in memory.
     * @return an unmodifiable List of Category objects.
     */
    public List<Category> getAllCategories() {
        return categories;
    }

    /**
     * Returns an ObservableList of all categories, used for UI bindings.
     * @return the ObservableList of Category objects.
     */
    public ObservableList<Category> getObservableCategories() {
        return categories;
    }

    /**
     * Creates a new Category with the given name and adds it to the internal list.
     * @param name the name of the new category
     * @return the newly created Category object
     */
    public Category createCategory(String name) {
        Category cat = new Category(name);
        categories.add(cat);
        return cat;
    }

    /**
     * Renames an existing Category by setting a new name.
     * @param category the Category object to rename
     * @param newName  the new name
     */
    public void renameCategory(Category category, String newName) {
        category.setName(newName);
    }

    /**
     * Deletes the specified category and removes all tasks belonging to it,
     * along with any reminders linked to those tasks.
     * @param category the Category to delete
     */
    public void deleteCategory(Category category) {
        List<String> taskIdsToRemove = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getCategoryId() != null && t.getCategoryId().equals(category.getId())) {
                taskIdsToRemove.add(t.getId());
            }
        }
        // Remove reminders for those tasks
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
     * Returns a List of all priorities.
     * @return a List of Priority objects
     */
    public List<Priority> getAllPriorities() {
        return priorities;
    }

    /**
     * Returns an ObservableList of all priorities, used for UI bindings.
     * @return the ObservableList of Priority objects
     */
    public ObservableList<Priority> getObservablePriorities() {
        return priorities;
    }

    /**
     * Creates a new Priority with the given name and adds it to the internal list.
     * @param name the name of the new priority
     * @return the newly created Priority
     */
    public Priority createPriority(String name) {
        Priority p = new Priority(name);
        priorities.add(p);
        return p;
    }

    /**
     * Renames a Priority (ignored if it is the "Default" priority).
     * @param priority the Priority to rename
     * @param newName  the new name
     */
    public void renamePriority(Priority priority, String newName) {
        Priority def = getDefaultPriority();
        if (priority.getId().equals(def.getId())) {
            return; // do nothing if it's Default
        }
        priority.setName(newName);
    }

    /**
     * Deletes a Priority. If it is the default priority, nothing happens.
     * If it's not default, all tasks using it are reassigned to the default priority,
     * and the given priority is removed.
     * @param priority the Priority to delete
     */
    public void deletePriority(Priority priority) {
        Priority def = getDefaultPriority();
        if (priority.getId().equals(def.getId())) {
            return; // Δεν επιτρέπεται η διαγραφή του default
        }
        for (Task t : tasks) {
            if (t.getPriorityId().equals(priority.getId())) {
                t.setPriorityId(def.getId()); // Με αυτό το setPriorityId το binding θα ενημερώσει το UI
            }
        }
        priorities.remove(priority);
    }

    /**
     * Gets the default priority object.
     * @return the Priority that is considered "Default"
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
     * Returns a list of all tasks currently loaded.
     * @return an unmodifiable list of Task objects
     */
    public List<Task> getAllTasks() {
        return tasks;
    }

    /**
     * Returns an ObservableList of all tasks, used for UI bindings.
     * @return the ObservableList of Task objects
     */
    public ObservableList<Task> getObservableTasks() {
        return tasks;
    }

    /**
     * Creates a new Task with the provided data and adds it to the internal list.
     * @param title the title of the task
     * @param description the description
     * @param category the Category (can be null)
     * @param priority the Priority (can be null -> use Default)
     * @param deadline the deadline (LocalDate) or null
     * @return the newly created Task object
     */
    public Task createTask(String title, String description,
                           Category category, Priority priority,
                           LocalDate deadline) {
        String categoryId = (category != null) ? category.getId() : null;
        String priorityId = (priority != null) ? priority.getId() : getDefaultPriority().getId();

        Task task = new Task(title, description, categoryId, priorityId, deadline);
        tasks.add(task);
        return task;
    }

    /**
     * Updates the fields of an existing Task (title, desc, category, priority, deadline, status).
     * If the new status is COMPLETED and the old status was not,
     * all reminders of that task are removed.
     *
     * @param task the Task to update
     * @param newTitle new title
     * @param newDesc new description
     * @param newCategory new Category (null if none)
     * @param newPriority new Priority (null => default)
     * @param newDeadline new deadline date
     * @param newStatus new TaskStatus
     */
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

        // If we just transitioned to COMPLETED from another status, remove reminders
        if (previousStatus != TaskStatus.COMPLETED && newStatus == TaskStatus.COMPLETED) {
            reminders.removeIf(r -> r.getTaskId().equals(task.getId()));
            System.out.println("✅ All reminders for task '" + task.getTitle() + "' have been deleted.");
        }

        // Ελέγχουμε αν η εργασία πρέπει να γίνει DELAYED (σε περίπτωση που άλλαξε deadline)
        task.checkIfShouldBeDelayed();
    }

    /**
     * Deletes the given Task and all its associated Reminders.
     * @param task the Task to delete
     */
    public void deleteTask(Task task) {
        reminders.removeIf(r -> r.getTaskId().equals(task.getId()));
        tasks.remove(task);
    }

    // ---------------------------------------------------------------
    // Reminders
    // ---------------------------------------------------------------

    /**
     * Returns a List of all reminders.
     * @return a list of Reminder objects
     */
    public List<Reminder> getAllReminders() {
        return reminders;
    }

    /**
     * Returns an ObservableList of all reminders, used for UI bindings.
     * @return the ObservableList of Reminder objects
     */
    public ObservableList<Reminder> getObservableReminders() {
        return reminders;
    }

    /**
     * Creates a new Reminder for a given Task, checking constraints such as:
     * - The task must not be COMPLETED
     * - If type != SPECIFIC_DATE, the task must have a deadline
     * - The computed or specified reminder date must not be in the past
     * - Αν είναι SPECIFIC_DATE, πρέπει να είναι πριν το deadline και >= σήμερα
     *
     * @param task the Task
     * @param type the ReminderType (ONE_DAY_BEFORE, etc.)
     * @param customDate a LocalDate if type == SPECIFIC_DATE
     * @return the newly created Reminder object
     * @throws IllegalStateException if the task is completed
     * @throws IllegalArgumentException if the date is invalid or in the past
     */
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

        // Έλεγχος: να μην είναι στο παρελθόν και (αν SPECIFIC_DATE) να μην είναι >= deadline
        if (reminderDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reminder date cannot be in the past.");
        }
        // Αν είναι specific, βεβαιωνόμαστε ότι είναι πριν το deadline (strictly)
        if (type == ReminderType.SPECIFIC_DATE && deadline != null) {
            if (!reminderDate.isBefore(deadline)) {
                throw new IllegalArgumentException("Reminder date must be strictly before the Task's deadline.");
            }
        }

        Reminder reminder = new Reminder(task.getId(), type, reminderDate);
        reminders.add(reminder);
        return reminder;
    }

    /**
     * Deletes a specific Reminder.
     * @param reminder the Reminder to delete
     */
    public void deleteReminder(Reminder reminder) {
        reminders.remove(reminder);
    }

    /**
     * Updates an existing Reminder with new Task, type, and date constraints.
     *
     * @param reminder the Reminder to update
     * @param newTask the new Task to associate with
     * @param newType the new ReminderType
     * @param newDate the new LocalDate if type == SPECIFIC_DATE
     * @throws IllegalArgumentException if the date is invalid or in the past
     */
    public void updateReminder(Reminder reminder,
                               Task newTask,
                               ReminderType newType,
                               LocalDate newDate) {
        if (reminder == null) return;

        if (newType == ReminderType.SPECIFIC_DATE && newDate == null) {
            throw new IllegalArgumentException("Reminder date cannot be empty for SPECIFIC_DATE.");
        }

        if (newTask.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot set reminder for a Completed task.");
        }

        LocalDate deadline = newTask.getDeadline();
        if (deadline == null && newType != ReminderType.SPECIFIC_DATE) {
            throw new IllegalArgumentException("Task has no deadline, cannot set this type of reminder.");
        }

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

        if (reminderDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Reminder date cannot be in the past.");
        }
        if (newType == ReminderType.SPECIFIC_DATE && deadline != null) {
            if (!reminderDate.isBefore(deadline)) {
                throw new IllegalArgumentException("Reminder date must be strictly before the Task's deadline.");
            }
        }

        reminder.setTaskId(newTask.getId());
        reminder.setType(newType);
        reminder.setReminderDate(reminderDate);

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

    /**
     * Searches tasks by optional title (substring match), category, and priority.
     *
     * @param title partial title to match (ignore case)
     * @param category category to match (or null => no filter)
     * @param priority priority to match (or null => no filter)
     * @return a List of Task objects matching the given criteria
     */
    public List<Task> searchTasks(String title, Category category, Priority priority) {
        return tasks.stream().filter(task -> {
            boolean matchTitle = true;
            boolean matchCategory = true;
            boolean matchPriority = true;

            if (title != null && !title.isEmpty()) {
                matchTitle = task.getTitle() != null
                        && task.getTitle().toLowerCase().contains(title.toLowerCase());
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

    /**
     * Αναζήτηση αποκλειστικά για tasks που ΔΕΝ έχουν κατηγορία,
     * με επιπλέον φίλτρο προαιρετικού τίτλου (partial) και προαιρετικού priority.
     *
     * @param title partial title
     * @param priority (ή null -> no filter)
     * @return λίστα με tasks χωρίς categoryId
     */
    public List<Task> searchTasksNoCategory(String title, Priority priority) {
        return tasks.stream().filter(task -> {
            boolean matchTitle = true;
            boolean matchPriority = true;
            boolean hasNoCategory = (task.getCategoryId() == null);

            if (title != null && !title.isEmpty()) {
                matchTitle = task.getTitle() != null
                        && task.getTitle().toLowerCase().contains(title.toLowerCase());
            }
            if (priority != null) {
                matchPriority = priority.getId().equals(task.getPriorityId());
            }

            return hasNoCategory && matchTitle && matchPriority;
        }).toList();
    }

    // ---------------------------------------------------------------
    // Helper lookups
    // ---------------------------------------------------------------

    /**
     * Finds a Category by its unique ID.
     * @param categoryId the ID of the Category
     * @return the Category object, or null if not found
     */
    public Category findCategoryById(String categoryId) {
        if (categoryId == null) return null;
        return categories.stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a Priority by its unique ID.
     * @param priorityId the ID of the Priority
     * @return the Priority object, or null if not found
     */
    public Priority findPriorityById(String priorityId) {
        if (priorityId == null) return null;
        return priorities.stream()
                .filter(p -> p.getId().equals(priorityId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a Task by its unique ID.
     * @param taskId the ID of the Task
     * @return the Task object, or null if not found
     */
    public Task getTaskById(String taskId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------
    // Private Helpers
    // ---------------------------------------------------------------

    /**
     * Ensures that a "Default" priority exists, or creates one if missing.
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
     * Checks if any tasks need to be labeled "DELAYED" (deadline passed, not completed).
     */
    private void updateDelayedTasks() {
        for (Task t : tasks) {
            t.checkIfShouldBeDelayed();
        }
    }
}

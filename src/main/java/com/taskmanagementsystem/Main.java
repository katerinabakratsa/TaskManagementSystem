package com.taskmanagementsystem;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Κύρια κλάση JavaFX που ξεκινά την εφαρμογή "MediaLab Assistant".
 */
public class Main extends Application {

    private TaskManager taskManager = new TaskManager();

    // Με αυτό το path θα αποθηκεύουμε/φορτώνουμε δεδομένα
    private final String JSON_FILE_PATH = "medialab/data.json";

    // Ετικέτες περίληψης (πάνω μέρος)
    private Label lblTotalTasks;
    private Label lblCompletedTasks;
    private Label lblDelayedTasks;

    @Override
    public void start(Stage primaryStage) {
        // 1. Φόρτωση δεδομένων από JSON
        taskManager.loadDataFromJSON(JSON_FILE_PATH);

        // 2. Ελέγχουμε αν κάποια task έχουν λήξει
        taskManager.checkAndUpdateDelayedTasks();

        // 3. Φτιάχνουμε το βασικό layout
        BorderPane root = new BorderPane();

        // ----------------------------------------------------
        // Πάνω μέρος: Συνολικά στοιχεία
        // ----------------------------------------------------
        HBox topSection = new HBox(20);
        topSection.setPadding(new Insets(10));

        lblTotalTasks = new Label("Total tasks: 0");
        lblCompletedTasks = new Label("Completed tasks: 0");
        lblDelayedTasks = new Label("Delayed tasks: 0");

        topSection.getChildren().addAll(lblTotalTasks, lblCompletedTasks, lblDelayedTasks);
        root.setTop(topSection);

        // ----------------------------------------------------
        // Κεντρικό μέρος: Φόρμες και λειτουργίες
        // ----------------------------------------------------
        VBox centerSection = new VBox(10);
        centerSection.setPadding(new Insets(10));

        // --- Περιοχή για Διαχείριση Εργασιών ---
        Label taskSectionLabel = new Label("Διαχείριση Εργασιών");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Enter task description");

        ComboBox<String> categoryComboBox = new ComboBox<>();
        // Παραδείγματα σταθερών κατηγοριών (αν θες δυναμικές, χρειάζεται επιπλέον λογική)
        categoryComboBox.getItems().addAll("Work", "Personal", "Urgent");

        ComboBox<String> priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll("Low", "Medium", "High");

        TextField deadlineField = new TextField();
        deadlineField.setPromptText("Enter deadline (yyyy-MM-dd)");

        Button addTaskButton = new Button("Add Task");
        addTaskButton.setOnAction(e -> {
            handleAddTask(titleField, descriptionField, categoryComboBox, priorityComboBox, deadlineField);
        });

        Button editTaskButton = new Button("Edit Task");
        editTaskButton.setOnAction(e -> {
            handleEditTask(titleField, descriptionField, categoryComboBox, priorityComboBox, deadlineField);
        });

        Button deleteTaskButton = new Button("Delete Task");
        deleteTaskButton.setOnAction(e -> {
            handleDeleteTask(titleField);
        });

        // ---- Αναζήτηση Εργασιών ----
        Label searchLabel = new Label("Αναζήτηση Εργασιών:");
        HBox searchBox = new HBox(5);
        TextField searchTitleField = new TextField();
        searchTitleField.setPromptText("Search by title");
        ComboBox<String> searchCategoryComboBox = new ComboBox<>();
        searchCategoryComboBox.getItems().addAll("Work", "Personal", "Urgent");
        ComboBox<String> searchPriorityComboBox = new ComboBox<>();
        searchPriorityComboBox.getItems().addAll("Low", "Medium", "High");

        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> handleSearchTasks(searchTitleField, searchCategoryComboBox, searchPriorityComboBox));
        searchBox.getChildren().addAll(searchTitleField, searchCategoryComboBox, searchPriorityComboBox, searchButton);

        // ---- Διαχείριση Υπενθυμίσεων ----
        Label reminderLabel = new Label("Υπενθυμίσεις:");
        ListView<String> reminderListView = new ListView<>();

        Button refreshRemindersButton = new Button("Refresh Reminders");
        refreshRemindersButton.setOnAction(e -> refreshReminders(reminderListView));

        HBox addReminderBox = new HBox(5);
        TextField reminderTaskTitleField = new TextField();
        reminderTaskTitleField.setPromptText("Enter task title for reminder");

        ComboBox<String> reminderTypeComboBox = new ComboBox<>();
        reminderTypeComboBox.getItems().addAll("1 day before", "1 week before", "Custom date");

        TextField reminderDateField = new TextField();
        reminderDateField.setPromptText("Enter custom date (yyyy-MM-dd)");

        Button addReminderButton = new Button("Add Reminder");
        addReminderButton.setOnAction(e -> {
            handleAddReminder(reminderTaskTitleField, reminderTypeComboBox, reminderDateField, reminderListView);
        });

        Button deleteCompletedRemindersButton = new Button("Delete Completed Task Reminders");
        deleteCompletedRemindersButton.setOnAction(e -> {
            taskManager.deleteRemindersForCompletedTasks();
            refreshReminders(reminderListView);
        });

        addReminderBox.getChildren().addAll(reminderTaskTitleField, reminderTypeComboBox, reminderDateField, addReminderButton);

        // Προσθήκη όλων στο κεντρικό VBox
        centerSection.getChildren().addAll(
            taskSectionLabel,
            titleField, descriptionField, categoryComboBox, priorityComboBox, deadlineField,
            new HBox(5, addTaskButton, editTaskButton, deleteTaskButton),

            searchLabel,
            searchBox,

            reminderLabel,
            reminderListView,
            new HBox(5, refreshRemindersButton, deleteCompletedRemindersButton),
            new Label("Add Reminder:"),
            addReminderBox
        );

        root.setCenter(centerSection);

        // 4. Φτιάχνουμε τη σκηνή και δείχνουμε το παράθυρο
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("MediaLab Assistant");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 5. Ενημέρωση των labels (σύνολο, completed, delayed) βάσει των φορτωμένων δεδομένων
        updateSummaryLabels();
    }

    @Override
    public void stop() {
        // Αποθήκευση δεδομένων σε JSON πριν τον τερματισμό
        taskManager.saveDataToJSON(JSON_FILE_PATH);
    }

    // ----------------------------------------------------------
    // Βοηθητικές μέθοδοι για τα χειριστήρια
    // ----------------------------------------------------------

    /** Προσθήκη νέας εργασίας **/
    private void handleAddTask(
            TextField titleField,
            TextField descriptionField,
            ComboBox<String> categoryComboBox,
            ComboBox<String> priorityComboBox,
            TextField deadlineField
    ) {
        try {
            String title = titleField.getText();
            String description = descriptionField.getText();
            String category = categoryComboBox.getValue();
            String priority = priorityComboBox.getValue();

            LocalDate deadline = LocalDate.parse(deadlineField.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            Task newTask = new Task(title, description, category, priority, deadline);
            taskManager.addTask(newTask);

            showAlert("Success", "Task Added!");
            updateSummaryLabels();
        } catch (Exception e) {
            showAlert("Error", "Invalid input for task creation.\n" + e.getMessage());
        }
    }

    /** Επεξεργασία υπάρχουσας εργασίας (με κριτήριο το title) **/
    private void handleEditTask(
            TextField titleField,
            TextField descriptionField,
            ComboBox<String> categoryComboBox,
            ComboBox<String> priorityComboBox,
            TextField deadlineField
    ) {
        try {
            String oldTitle = titleField.getText();  // Το αρχικό title
            String newTitle = titleField.getText();
            String newDesc = descriptionField.getText();
            String newCategory = categoryComboBox.getValue();
            String newPriority = priorityComboBox.getValue();
            LocalDate newDeadline = LocalDate.parse(deadlineField.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            Task updatedTask = new Task(newTitle, newDesc, newCategory, newPriority, newDeadline);

            // Αν θέλουμε να αλλάζουμε status, θα το κάναμε εδώ, π.χ. updatedTask.setStatus("In Progress") ή κάτι άλλο
            // Προς το παρόν αφήνουμε το default "Open" ή αν θέλεις ο χρήστης να εισάγει status, προσθέτεις ComboBox κτλ.
            updatedTask.setStatus("Open");

            taskManager.updateTask(oldTitle, updatedTask);

            showAlert("Success", "Task Updated!");
            updateSummaryLabels();
        } catch (Exception e) {
            showAlert("Error", "Invalid input for task update.\n" + e.getMessage());
        }
    }

    /** Διαγραφή εργασίας **/
    private void handleDeleteTask(TextField titleField) {
        String title = titleField.getText();
        if (title == null || title.isEmpty()) {
            showAlert("Warning", "Please enter a task title to delete.");
            return;
        }
        taskManager.deleteTask(title);
        showAlert("Success", "Task Deleted!");
        updateSummaryLabels();
    }

    /** Αναζήτηση εργασιών **/
    private void handleSearchTasks(
            TextField searchTitleField,
            ComboBox<String> searchCategoryComboBox,
            ComboBox<String> searchPriorityComboBox
    ) {
        String title = searchTitleField.getText();
        String category = searchCategoryComboBox.getValue();
        String priority = searchPriorityComboBox.getValue();

        if (title == null) title = "";
        if (category == null) category = "";
        if (priority == null) priority = "";

        List<Task> results = taskManager.searchTasks(title, category, priority);

        // Εμφάνιση αποτελεσμάτων στο console ή σε popup
        if (results.isEmpty()) {
            showAlert("Search Results", "No tasks found matching the criteria.");
        } else {
            StringBuilder sb = new StringBuilder("Found " + results.size() + " tasks:\n");
            for (Task t : results) {
                sb.append("- ").append(t.getTitle())
                  .append(" [").append(t.getCategory()).append(", ")
                  .append(t.getPriority()).append("]\n");
            }
            showAlert("Search Results", sb.toString());
        }
    }

    /** Ενημέρωση λίστας υπενθυμίσεων **/
    private void refreshReminders(ListView<String> reminderListView) {
        reminderListView.getItems().clear();
        for (Reminder reminder : taskManager.getAllReminders()) {
            // π.χ. "ΤίτλοςTask - 1 day before (2025-12-13)"
            String line = reminder.getTask().getTitle() + " - " +
                          reminder.getType() + " (" + reminder.getReminderDate() + ")";
            reminderListView.getItems().add(line);
        }
    }

    /** Προσθήκη υπενθύμισης **/
    private void handleAddReminder(
            TextField taskTitleField,
            ComboBox<String> typeComboBox,
            TextField dateField,
            ListView<String> reminderListView
    ) {
        try {
            String taskTitle = taskTitleField.getText();
            String type = typeComboBox.getValue();
            if (taskTitle == null || taskTitle.isEmpty()) {
                showAlert("Error", "Please enter a valid task title for reminder.");
                return;
            }
            // Εντοπίζουμε το Task με βάση τον τίτλο
            Task task = taskManager.getAllTasks().stream()
                    .filter(t -> t.getTitle().equals(taskTitle))
                    .findFirst()
                    .orElse(null);

            if (task == null) {
                showAlert("Error", "Task not found: " + taskTitle);
                return;
            }

            // Υπολογίζουμε το reminderDate αναλόγως του type
            LocalDate reminderDate;
            if ("1 day before".equals(type)) {
                reminderDate = task.getDeadline().minusDays(1);
            } else if ("1 week before".equals(type)) {
                reminderDate = task.getDeadline().minusWeeks(1);
            } else {
                // Custom date
                reminderDate = LocalDate.parse(dateField.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }

            // Προσθέτουμε στο manager
            taskManager.addReminder(task, type, reminderDate.toString());
            refreshReminders(reminderListView);
            showAlert("Success", "Reminder added successfully!");
        } catch (Exception e) {
            showAlert("Error", "Invalid input for reminder creation.\n" + e.getMessage());
        }
    }

    /** Ενημέρωση των τριών ετικετών (πάνω μέρος) */
    private void updateSummaryLabels() {
        int total = taskManager.getAllTasks().size();
        int completed = (int) taskManager.getAllTasks().stream()
                .filter(task -> "Completed".equals(task.getStatus()))
                .count();
        int delayed = (int) taskManager.getAllTasks().stream()
                .filter(task -> "Delayed".equals(task.getStatus()))
                .count();

        lblTotalTasks.setText("Total tasks: " + total);
        lblCompletedTasks.setText("Completed tasks: " + completed);
        lblDelayedTasks.setText("Delayed tasks: " + delayed);
    }

    /** Εμφάνιση απλού popup Alert */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.taskmanagementsystem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class MainApplication extends Application {

    private DataManager dataManager = new DataManager();

    // Labels for top summary
    private Label lblTotalTasks;
    private Label lblCompletedTasks;
    private Label lblDelayedTasks;
    private Label lblDeadline7Days;

    // Αναφορά στο TableView των tasks (για να το ανανεώνουμε σε φίλτρα κ.λπ.)
    private TableView<Task> tasksTable;

    // Ειδική “dummy” κατηγορία για την επιλογή "All Categories" στο Tasks tab
    private Category allCategoryPlaceholder;

    // Το ComboBox για το filter στο Tasks tab
    private ComboBox<Category> cmbFilterCategory;

    // Θα διατηρούμε μια ξεχωριστή λίστα που περιέχει:
    // [“All Categories” + (όλες τις πραγματικές από το dataManager)]
    private final ObservableList<Category> combinedFilterCategories = FXCollections.observableArrayList();

    // ---- Search Tab controls (για να κάνουμε refresh αυτόματα) ----
    private TextField txtSearchTitle;
    private ComboBox<Category> cmbSearchCategory;
    private ComboBox<Priority> cmbSearchPriority;
    private TableView<Task> searchTable;         // πίνακας αναζήτησης
    private FilteredList<Task> filteredTasks;    // φίλτρο αναζήτησης πάνω στα tasks

    @Override
    public void start(Stage primaryStage) {
        // 1. Load data from JSON
        dataManager.loadAllData();

        // 2. Ενημέρωση εκπρόθεσμων εργασιών
        for (Task task : dataManager.getAllTasks()) {
            task.checkIfShouldBeDelayed();
        }

        // -- Δημιουργούμε το κύριο layout
        BorderPane root = new BorderPane();

        // TOP: summary info
        VBox topBox = createTopBox();
        root.setTop(topBox);

        // CENTER: TabPane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tasksTab = new Tab("Tasks", createTasksPane());
        Tab categoriesTab = new Tab("Categories", createCategoriesPane());
        Tab prioritiesTab = new Tab("Priorities", createPrioritiesPane());
        Tab remindersTab = new Tab("Reminders", createRemindersPane());
        Tab searchTab = new Tab("Search", createSearchPane());

        tabPane.getTabs().addAll(tasksTab, categoriesTab, prioritiesTab, remindersTab, searchTab);
        root.setCenter(tabPane);

        // 3. Φτιάχνουμε Scene, δείχνουμε παράθυρο
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setTitle("MediaLab Assistant");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 4. Αρχική ενημέρωση counters
        updateSummaryInfo();

        // 5. Εμφάνιση popup καθυστερημένων εργασιών, αφού έχει εμφανιστεί το παράθυρο
        Platform.runLater(() -> {
            long delayedCount = dataManager.getAllTasks().stream()
                    .filter(t -> t.getStatus() == TaskStatus.DELAYED)
                    .count();
            if (delayedCount > 0) {
                showAlert("Delayed Tasks", "There are " + delayedCount + " delayed tasks!");
            }
        });
    }

    @Override
    public void stop() {
        // Αποθήκευση JSON πριν τον τερματισμό
        dataManager.saveAllData();
    }

    // ---------------------------------------------------------------
    // TOP BOX
    // ---------------------------------------------------------------
    private VBox createTopBox() {
        Label lblTitle = new Label("MediaLab Assistant");
        lblTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        lblTotalTasks = new Label("Total tasks: 0");
        lblCompletedTasks = new Label("Completed tasks: 0");
        lblDelayedTasks = new Label("Delayed tasks: 0");
        lblDeadline7Days = new Label("Due <= 7 days: 0");

        String counterStyle = "-fx-font-size: 14px; -fx-font-weight: bold;";
        lblTotalTasks.setStyle(counterStyle);
        lblCompletedTasks.setStyle(counterStyle);
        lblDelayedTasks.setStyle(counterStyle);
        lblDeadline7Days.setStyle(counterStyle);

        HBox countersBox = new HBox(20, lblTotalTasks, lblCompletedTasks, lblDelayedTasks, lblDeadline7Days);
        countersBox.setAlignment(Pos.CENTER);

        VBox topBox = new VBox();
        topBox.setSpacing(5);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER);
        topBox.getChildren().addAll(lblTitle, countersBox);

        return topBox;
    }

    /**
     * Ενημερώνουμε τους μετρητές (συνολικών tasks, completed, delayed, dueIn7)
     */
    private void updateSummaryInfo() {
        List<Task> allTasks = dataManager.getAllTasks();
        int total = allTasks.size();
        long completed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long delayed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.DELAYED).count();
        long dueIn7 = allTasks.stream().filter(t -> {
            if (t.getDeadline() == null) return false;
            if (t.getStatus() == TaskStatus.COMPLETED) return false;
            return !t.getDeadline().isBefore(LocalDate.now().minusDays(1))
                    && !t.getDeadline().isAfter(LocalDate.now().plusDays(7));
        }).count();

        lblTotalTasks.setText("Total tasks: " + total);
        lblCompletedTasks.setText("Completed tasks: " + completed);
        lblDelayedTasks.setText("Delayed tasks: " + delayed);
        lblDeadline7Days.setText("Due <= 7 days: " + dueIn7);
    }

    // ---------------------------------------------------------------
    // TAB 1: TASKS (με φίλτρο κατηγορίας “All Categories”)
    // ---------------------------------------------------------------
    private Pane createTasksPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        // Δημιουργούμε την placeholder κατηγορία
        allCategoryPlaceholder = new Category("All Categories");
        allCategoryPlaceholder.setId("ALL");

        // Φτιάχνουμε την combinedFilterCategories (All + dataManager categories)
        updateFilterCategoriesList();

        // Φίλτρο
        HBox filterBox = new HBox(10);
        filterBox.setPadding(new Insets(0, 0, 10, 0));
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label lblFilter = new Label("Filter by Category:");

        cmbFilterCategory = new ComboBox<>(combinedFilterCategories);
        cmbFilterCategory.setValue(allCategoryPlaceholder); // Αρχικά εμφανίζει “All Categories”
        cmbFilterCategory.setConverter(ConverterUtils.getCategoryConverter());

        tasksTable = new TableView<>();
        tasksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Task, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Task, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Task, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tasksTable.getColumns().addAll(colTitle, colDesc, colStatus);
        tasksTable.setItems(dataManager.getObservableTasks());

        // Όταν αλλάξει το value στο ComboBox, φιλτράρουμε
        cmbFilterCategory.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyCategoryFilter();
        });

        filterBox.getChildren().addAll(lblFilter, cmbFilterCategory);

        // FORM (Add/Edit)
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(5));

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Title");
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Description");

        ComboBox<Category> cmbCategory = new ComboBox<>(dataManager.getObservableCategories());
        cmbCategory.setPromptText("Select Category");
        cmbCategory.setConverter(ConverterUtils.getCategoryConverter());

        ComboBox<Priority> cmbPriority = new ComboBox<>(dataManager.getObservablePriorities());
        cmbPriority.setPromptText("Select Priority");
        cmbPriority.setConverter(ConverterUtils.getPriorityConverter());

        DatePicker dpDeadline = new DatePicker();
        dpDeadline.setPromptText("Deadline");

        ComboBox<TaskStatus> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(TaskStatus.values()));
        cmbStatus.setPromptText("Status");

        Button btnAdd = new Button("Add");
        btnAdd.setOnAction(e -> {
            try {
                Category cat = cmbCategory.getValue();
                Priority prio = cmbPriority.getValue();
                LocalDate dl = dpDeadline.getValue();
                Task t = dataManager.createTask(txtTitle.getText(), txtDesc.getText(), cat, prio, dl);

                if (cmbStatus.getValue() != null) {
                    t.setStatus(cmbStatus.getValue());
                }

                showAlert("Success", "Task created successfully!");
                refreshAllTablesAndCounters();

                // Καθαρισμός
                txtTitle.clear();
                txtDesc.clear();
                cmbCategory.setValue(null);
                cmbPriority.setValue(null);
                dpDeadline.setValue(null);
                cmbStatus.setValue(null);

                // Εφαρμόζουμε ξανά το φίλτρο
                applyCategoryFilter();

            } catch (Exception ex) {
                showAlert("Error", "Could not create task: " + ex.getMessage());
            }
        });

        Button btnUpdate = new Button("Update");
        btnUpdate.setOnAction(e -> {
            Task selected = tasksTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a task from the table first.");
                return;
            }
            Category cat = cmbCategory.getValue();
            Priority prio = cmbPriority.getValue();
            LocalDate dl = dpDeadline.getValue();
            TaskStatus st = (cmbStatus.getValue() != null) ? cmbStatus.getValue() : selected.getStatus();

            dataManager.updateTask(selected,
                    txtTitle.getText(),
                    txtDesc.getText(),
                    cat,
                    prio,
                    dl,
                    st);

            showAlert("Success", "Task updated!");
            refreshAllTablesAndCounters();
            applyCategoryFilter();
        });

        Button btnDelete = new Button("Delete");
        btnDelete.setOnAction(e -> {
            Task selected = tasksTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a task from the table first.");
                return;
            }
            dataManager.deleteTask(selected);
            showAlert("Success", "Task deleted.");
            refreshAllTablesAndCounters();
            applyCategoryFilter();
        });

        tasksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtTitle.setText(newVal.getTitle());
                txtDesc.setText(newVal.getDescription());
                Category c = dataManager.findCategoryById(newVal.getCategoryId());
                cmbCategory.setValue(c);
                Priority p = dataManager.findPriorityById(newVal.getPriorityId());
                cmbPriority.setValue(p);
                dpDeadline.setValue(newVal.getDeadline());
                cmbStatus.setValue(newVal.getStatus());
            }
        });

        formBox.getChildren().addAll(
                new Label("Manage Task"),
                txtTitle, txtDesc, cmbCategory, cmbPriority, dpDeadline, cmbStatus,
                new HBox(10, btnAdd, btnUpdate, btnDelete)
        );

        VBox topContainer = new VBox(filterBox, tasksTable);
        pane.setCenter(topContainer);
        pane.setRight(formBox);

        return pane;
    }

    /**
     * Εφαρμόζει το φίλτρο κατηγορίας στον πίνακα εργασιών.
     */
    private void applyCategoryFilter() {
        Category selectedCat = cmbFilterCategory.getValue();
        if (selectedCat == null || "ALL".equals(selectedCat.getId())) {
            // "All Categories"
            tasksTable.setItems(dataManager.getObservableTasks());
        } else {
            // Φιλτράρουμε
            List<Task> filtered = dataManager.getAllTasks().stream()
                    .filter(t -> selectedCat.getId().equals(t.getCategoryId()))
                    .collect(Collectors.toList());
            tasksTable.setItems(FXCollections.observableArrayList(filtered));
        }
        tasksTable.refresh();
    }

    /**
     * Ανανεώνει τη λίστα combinedFilterCategories,
     * ώστε να περιέχει πάντα "All Categories" + τις κανονικές κατηγορίες από το dataManager.
     */
    private void updateFilterCategoriesList() {
        combinedFilterCategories.clear();
        // Πρώτα “All Categories”
        allCategoryPlaceholder = new Category("All Categories");
        allCategoryPlaceholder.setId("ALL");
        combinedFilterCategories.add(allCategoryPlaceholder);

        // Μετά οι πραγματικές από το DataManager
        combinedFilterCategories.addAll(dataManager.getObservableCategories());
    }

    // ---------------------------------------------------------------
    // TAB 2: CATEGORIES
    // ---------------------------------------------------------------
    private Pane createCategoriesPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        ListView<Category> listView = new ListView<>(dataManager.getObservableCategories());
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(5));

        TextField txtCategoryName = new TextField();
        txtCategoryName.setPromptText("New category name");

        Button btnAdd = new Button("Add Category");
        btnAdd.setOnAction(e -> {
            if (txtCategoryName.getText().isEmpty()) return;
            dataManager.createCategory(txtCategoryName.getText());
            txtCategoryName.clear();
            refreshAllTablesAndCounters();
            updateFilterCategoriesList();
        });

        Button btnRename = new Button("Rename Category");
        btnRename.setOnAction(e -> {
            Category selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Please select a category first.");
                return;
            }
            if (txtCategoryName.getText().isEmpty()) return;
            dataManager.renameCategory(selected, txtCategoryName.getText());
            txtCategoryName.clear();
            refreshAllTablesAndCounters();
            updateFilterCategoriesList();
        });

        Button btnDelete = new Button("Delete Category");
        btnDelete.setOnAction(e -> {
            Category selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a category first.");
                return;
            }
            dataManager.deleteCategory(selected);
            showAlert("Success", "Category and related tasks removed!");
            refreshAllTablesAndCounters();
            updateFilterCategoriesList();
        });

        formBox.getChildren().addAll(
                new Label("Category Name:"),
                txtCategoryName,
                new HBox(10, btnAdd, btnRename, btnDelete)
        );

        pane.setCenter(listView);
        pane.setRight(formBox);

        return pane;
    }

    // ---------------------------------------------------------------
    // TAB 3: PRIORITIES
    // ---------------------------------------------------------------
    private Pane createPrioritiesPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        ListView<Priority> listView = new ListView<>(dataManager.getObservablePriorities());
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Priority item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(5));

        TextField txtPrioName = new TextField();
        txtPrioName.setPromptText("Priority name");

        Button btnAdd = new Button("Add Priority");
        btnAdd.setOnAction(e -> {
            if (txtPrioName.getText().isEmpty()) return;
            dataManager.createPriority(txtPrioName.getText());
            txtPrioName.clear();
            refreshAllTablesAndCounters();
        });

        Button btnRename = new Button("Rename Priority");
        btnRename.setOnAction(e -> {
            Priority selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a priority first.");
                return;
            }
            if (txtPrioName.getText().isEmpty()) return;
            dataManager.renamePriority(selected, txtPrioName.getText());
            txtPrioName.clear();
            refreshAllTablesAndCounters();
        });

        Button btnDelete = new Button("Delete Priority");
        btnDelete.setOnAction(e -> {
            Priority selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a priority first.");
                return;
            }
            dataManager.deletePriority(selected);
            showAlert("Success", "Priority deleted or replaced with Default in tasks.");
            // Με το που γίνεται εδώ η διαγραφή, όσα tasks είχαν το priority 
            // ανατίθενται σε default. Καλούμε refreshAllTablesAndCounters()
            // για άμεση ενημέρωση στην οθόνη.
            refreshAllTablesAndCounters();
        });

        // Αν είναι το “Default”, κρύβουμε Rename/Delete
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getName().equalsIgnoreCase("Default")) {
                btnRename.setVisible(false);
                btnDelete.setVisible(false);
            } else {
                btnRename.setVisible(true);
                btnDelete.setVisible(true);
            }
        });

        formBox.getChildren().addAll(
                new Label("Priority:"),
                txtPrioName,
                new HBox(10, btnAdd, btnRename, btnDelete)
        );

        pane.setCenter(listView);
        pane.setRight(formBox);

        return pane;
    }

    // ---------------------------------------------------------------
    // TAB 4: REMINDERS
    // ---------------------------------------------------------------
    private Pane createRemindersPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        TableView<Reminder> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");

        TableColumn<Reminder, String> colTaskTitle = new TableColumn<>("Task Title");
        colTaskTitle.setCellValueFactory(cell -> {
            String taskId = cell.getValue().getTaskId();
            Task task = dataManager.getTaskById(taskId);
            return new SimpleStringProperty(task != null ? task.getTitle() : "N/A");
        });

        TableColumn<Reminder, ReminderType> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Reminder, String> colDate = new TableColumn<>("Reminder Date");
        colDate.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getReminderDate();
            return new SimpleStringProperty(date != null ? date.toString() : "N/A");
        });

        table.getColumns().addAll(colTaskTitle, colType, colDate);
        table.setItems(dataManager.getObservableReminders());

        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(5));

        ComboBox<Task> cmbTask = new ComboBox<>(dataManager.getObservableTasks());
        cmbTask.setPromptText("Select Task");
        cmbTask.setConverter(ConverterUtils.getTaskConverter());

        ComboBox<ReminderType> cmbType = new ComboBox<>(FXCollections.observableArrayList(ReminderType.values()));
        cmbType.setPromptText("Select Reminder Type");

        DatePicker dpCustomDate = new DatePicker();
        dpCustomDate.setPromptText("Select Date");
        dpCustomDate.setDisable(true);

        // SPECIFIC_DATE -> ενεργοποιείται το DatePicker
        cmbType.setOnAction(e -> {
            dpCustomDate.setDisable(cmbType.getValue() != ReminderType.SPECIFIC_DATE);
            if (dpCustomDate.isDisabled()) {
                dpCustomDate.setValue(null);
            }
        });

        Button btnAdd = new Button("Add Reminder");
        btnAdd.setOnAction(e -> {
            Task selectedTask = cmbTask.getValue();
            ReminderType selectedType = cmbType.getValue();
            LocalDate selectedDate = dpCustomDate.getValue();

            if (selectedTask == null || selectedType == null) {
                showAlert("Error", "Please select both a Task and Reminder Type.");
                return;
            }
            if (selectedType == ReminderType.SPECIFIC_DATE && selectedDate == null) {
                showAlert("Error", "Reminder date cannot be empty.");
                return;
            }

            try {
                dataManager.createReminder(selectedTask, selectedType, selectedDate);
                showAlert("Success", "Reminder added successfully!");
                refreshAllTablesAndCounters();
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        Button btnDelete = new Button("Delete Reminder");
        btnDelete.setOnAction(e -> {
            Reminder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a reminder first.");
                return;
            }
            dataManager.deleteReminder(selected);
            refreshAllTablesAndCounters();
        });

        Button btnEdit = new Button("Update Reminder");
        btnEdit.setOnAction(e -> {
            Reminder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a reminder first.");
                return;
            }

            Task selectedTask = cmbTask.getValue();
            ReminderType selectedType = cmbType.getValue();
            LocalDate selectedDate = dpCustomDate.getValue();

            if (selectedTask == null || selectedType == null) {
                showAlert("Error", "Please select a task and reminder type.");
                return;
            }
            if (selectedType == ReminderType.SPECIFIC_DATE && selectedDate == null) {
                showAlert("Error", "Reminder date cannot be empty.");
                return;
            }

            try {
                dataManager.updateReminder(selected, selectedTask, selectedType, selectedDate);
                showAlert("Success", "Reminder updated successfully!");
                refreshAllTablesAndCounters();
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Task relatedTask = dataManager.getTaskById(newVal.getTaskId());
                cmbTask.setValue(relatedTask);
                cmbType.setValue(newVal.getType());
                dpCustomDate.setValue(newVal.getReminderDate());
                dpCustomDate.setDisable(newVal.getType() != ReminderType.SPECIFIC_DATE);
            }
        });

        formBox.getChildren().addAll(
                new Label("Task:"), cmbTask,
                new Label("Reminder Type:"), cmbType,
                new Label("Custom Date (if applicable):"), dpCustomDate,
                new HBox(10, btnAdd, btnEdit, btnDelete)
        );

        pane.setCenter(table);
        pane.setRight(formBox);

        return pane;
    }

    // ---------------------------------------------------------------
    // TAB 5: SEARCH
    // ---------------------------------------------------------------
    private Pane createSearchPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        txtSearchTitle = new TextField();
        txtSearchTitle.setPromptText("Search by title (partial)");

        // Δημιουργούμε 2 "placeholder" κατηγορίες: ALL (no filter) και NONE (tasks χωρίς κατηγορία)
        Category allCategoryPlaceholder = new Category("All (No Filter)");
        allCategoryPlaceholder.setId("ALL");
        Category noneCategoryPlaceholder = new Category("No Category");
        noneCategoryPlaceholder.setId("NONE");

        // Συνδυάζουμε σε ένα combo τις δύο placeholder + όλες τις πραγματικές
        ObservableList<Category> searchCategories = FXCollections.observableArrayList();
        searchCategories.add(allCategoryPlaceholder);
        searchCategories.add(noneCategoryPlaceholder);
        searchCategories.addAll(dataManager.getObservableCategories());

        cmbSearchCategory = new ComboBox<>(searchCategories);
        cmbSearchCategory.setPromptText("Category");
        cmbSearchCategory.setConverter(ConverterUtils.getCategoryConverter());

        cmbSearchPriority = new ComboBox<>(dataManager.getObservablePriorities());
        cmbSearchPriority.setPromptText("Priority (optional)");
        cmbSearchPriority.setConverter(ConverterUtils.getPriorityConverter());

        // Χρησιμοποιούμε FilteredList ώστε οι αλλαγές στα tasks να εμφανίζονται αυτόματα
        filteredTasks = new FilteredList<>(dataManager.getObservableTasks(), t -> true);

        searchTable = new TableView<>(filteredTasks);
        searchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Task, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Task, String> colPrio = new TableColumn<>("Priority");
        colPrio.setCellValueFactory(cell -> {
            String pid = cell.getValue().getPriorityId();
            Priority p = dataManager.findPriorityById(pid);
            return new SimpleStringProperty(p != null ? p.getName() : "??");
        });

        TableColumn<Task, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(cell -> {
            String cid = cell.getValue().getCategoryId();
            Category c = dataManager.findCategoryById(cid);
            return new SimpleStringProperty(c != null ? c.getName() : "");
        });

        TableColumn<Task, String> colDeadline = new TableColumn<>("Deadline");
        colDeadline.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getDeadline();
            return new SimpleStringProperty(d != null ? d.toString() : "");
        });

        searchTable.getColumns().addAll(colTitle, colPrio, colCat, colDeadline);

        // Χρησιμοποιούμε listeners για να φιλτράρουμε αυτόματα κάθε φορά που αλλάζουν τα κριτήρια
        txtSearchTitle.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter());
        cmbSearchCategory.valueProperty().addListener((obs, oldVal, newVal) -> applySearchFilter());
        cmbSearchPriority.valueProperty().addListener((obs, oldVal, newVal) -> applySearchFilter());

        // Αρχικό φιλτράρισμα
        applySearchFilter();

        box.getChildren().addAll(
                new Label("Search Criteria:"),
                txtSearchTitle,
                cmbSearchCategory,
                cmbSearchPriority,
                new Label("Results:"),
                searchTable
        );

        return box;
    }

    /**
     * Εφαρμόζει το φίλτρο αναζήτησης στα tasks με βάση τα πεδία:
     * - txtSearchTitle
     * - cmbSearchCategory
     * - cmbSearchPriority
     */
    private void applySearchFilter() {
        Category cat = cmbSearchCategory.getValue();
        Priority prio = cmbSearchPriority.getValue();
        String titleFilter = txtSearchTitle.getText();

        filteredTasks.setPredicate(task -> {
            // Έλεγχος τίτλου (αν δοθεί)
            if (titleFilter != null && !titleFilter.isEmpty()) {
                if (task.getTitle() == null
                        || !task.getTitle().toLowerCase().contains(titleFilter.toLowerCase())) {
                    return false;
                }
            }
            // Έλεγχος κατηγορίας (ALL => no filter, NONE => only null category)
            if (cat != null && !"ALL".equals(cat.getId())) {
                if ("NONE".equals(cat.getId())) {
                    // μόνο tasks χωρίς category
                    if (task.getCategoryId() != null) return false;
                } else {
                    // συγκεκριμένη κατηγορία
                    if (!cat.getId().equals(task.getCategoryId())) {
                        return false;
                    }
                }
            }
            // Έλεγχος priority (αν επιλεγεί)
            if (prio != null) {
                if (!prio.getId().equals(task.getPriorityId())) {
                    return false;
                }
            }
            return true;
        });

        searchTable.refresh();
    }

    // ---------------------------------------------------------------
    // Utility: showAlert
    // ---------------------------------------------------------------
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Καλείται όταν γίνεται κάποια αλλαγή (π.χ. προσθήκη/διαγραφή task/priority κλπ)
     * για να κάνουμε refresh σε πίνακες, αναζήτηση και counters.
     */
    private void refreshAllTablesAndCounters() {
        updateSummaryInfo();

        // Ανανεώνουμε πίνακα tasks
        if (tasksTable != null) {
            tasksTable.refresh();
        }

        // Εφαρμόζουμε ξανά το φίλτρο αναζήτησης στο Search tab
        if (filteredTasks != null) {
            applySearchFilter();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

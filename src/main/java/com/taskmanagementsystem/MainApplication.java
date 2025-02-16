package com.taskmanagementsystem;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.taskmanagementsystem.ui.AnimatedBackground;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.AnimatedBackground;

/**
 * Main JavaFX Application that sets up the UI with TabPane for Tasks, Categories, Priorities, Reminders, Search.
 */
public class MainApplication extends Application {

    private DataManager dataManager = new DataManager();

    // Labels for top summary
    private Label lblTotalTasks;
    private Label lblCompletedTasks;
    private Label lblDelayedTasks;
    private Label lblDeadline7Days;

    @Override
    public void start(Stage primaryStage) {
        // 1. Load data from JSON
        dataManager.loadAllData();

    // 2. Ενημέρωση εκπρόθεσμων εργασιών
        for (Task task : dataManager.getAllTasks()) {
            task.checkIfShouldBeDelayed(); // Μετατρέπει αυτόματα σε DELAYED αν η προθεσμία έχει περάσει
        }
    
    // 3. Αποθήκευση αλλαγών πίσω στο JSON
        dataManager.saveAllData();

    // 4. Εμφάνιση μηνύματος αν υπάρχουν εκπρόθεσμες εργασίες
        long delayedCount = dataManager.getAllTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.DELAYED)
                .count();
        if (delayedCount > 0) {
            showAlert("Delayed Tasks", "There are " + delayedCount + " delayed tasks!");
       }

    // 5. Δημιουργία του κύριου UI
       BorderPane root = new BorderPane();


    
    // TOP: summary info
       HBox topBox = createTopBox();
       root.setTop(topBox);

    // CENTER: TabPane με όλες τις καρτέλες
        TabPane tabPane = new TabPane();
      tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE); // no close buttons

        Tab tasksTab = new Tab("Tasks", createTasksPane());
        Tab categoriesTab = new Tab("Categories", createCategoriesPane());
        Tab prioritiesTab = new Tab("Priorities", createPrioritiesPane());
        Tab remindersTab = new Tab("Reminders", createRemindersPane());
        Tab searchTab = new Tab("Search", createSearchPane());

        tabPane.getTabs().addAll(tasksTab, categoriesTab, prioritiesTab, remindersTab, searchTab);
       root.setCenter(tabPane);

    // 6. Δημιουργία Scene και εμφάνιση παραθύρου
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setTitle("MediaLab Assistant");
        primaryStage.setScene(scene);
        primaryStage.show();

    // 7. Αρχική ενημέρωση των counters
      updateSummaryInfo();
    }

    @Override
    public void stop() {
        // Save data to JSON
        dataManager.saveAllData();
    }

    // ---------------------------------------------------------------
    // TOP BOX
    // ---------------------------------------------------------------
    private HBox createTopBox() {
        HBox box = new HBox(30);
        box.setPadding(new Insets(10));
        lblTotalTasks = new Label("Total tasks: 0");
        lblCompletedTasks = new Label("Completed tasks: 0");
        lblDelayedTasks = new Label("Delayed tasks: 0");
        lblDeadline7Days = new Label("Due <= 7 days: 0");

        box.getChildren().addAll(lblTotalTasks, lblCompletedTasks, lblDelayedTasks, lblDeadline7Days);
        return box;
    }

    private void updateSummaryInfo() {
        List<Task> allTasks = dataManager.getAllTasks();
        int total = allTasks.size();
        long completed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long delayed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.DELAYED).count();
        long dueIn7 = allTasks.stream().filter(t -> {
            if (t.getDeadline() == null) return false;
            if (t.getStatus() == TaskStatus.COMPLETED) return false;
            return !t.getDeadline().isBefore(LocalDate.now().minusDays(1)) // don't count if it's already past
                    && !t.getDeadline().isAfter(LocalDate.now().plusDays(7));
        }).count();

        lblTotalTasks.setText("Total tasks: " + total);
        lblCompletedTasks.setText("Completed tasks: " + completed);
        lblDelayedTasks.setText("Delayed tasks: " + delayed);
        lblDeadline7Days.setText("Due <= 7 days: " + dueIn7);
    }

    // ---------------------------------------------------------------
    // TAB 1: TASKS
    // ---------------------------------------------------------------
    private Pane createTasksPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        // TABLE of tasks
        TableView<Task> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Task, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Task, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Task, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colTitle, colDesc, colStatus);

        table.setItems(FXCollections.observableArrayList(dataManager.getAllTasks()));

        // FORM (right side) for add/edit
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(5));

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Title");
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Description");

        ComboBox<Category> cmbCategory = new ComboBox<>(FXCollections.observableArrayList(dataManager.getAllCategories()));
        cmbCategory.setPromptText("Select Category");
        cmbCategory.setConverter(ConverterUtils.getCategoryConverter());

        ComboBox<Priority> cmbPriority = new ComboBox<>(FXCollections.observableArrayList(dataManager.getAllPriorities()));
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

                // If the user selected a status that is not null, set it
                if (cmbStatus.getValue() != null) {
                    t.setStatus(cmbStatus.getValue());
                }
                table.setItems(FXCollections.observableArrayList(dataManager.getAllTasks()));
                showAlert("Success", "Task created successfully!");
                updateSummaryInfo();
            } catch (Exception ex) {
                showAlert("Error", "Could not create task: " + ex.getMessage());
            }
        });

        Button btnUpdate = new Button("Update");
        btnUpdate.setOnAction(e -> {
            Task selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a task from the table first.");
                return;
            }
            Category cat = cmbCategory.getValue();
            Priority prio = cmbPriority.getValue();
            LocalDate dl = dpDeadline.getValue();
            TaskStatus st = cmbStatus.getValue() != null ? cmbStatus.getValue() : selected.getStatus();

            dataManager.updateTask(
                    selected,
                    txtTitle.getText(),
                    txtDesc.getText(),
                    cat,
                    prio,
                    dl,
                    st
            );
            table.setItems(FXCollections.observableArrayList(dataManager.getAllTasks()));
            showAlert("Success", "Task updated!");
            updateSummaryInfo();
        });

        Button btnDelete = new Button("Delete");
        btnDelete.setOnAction(e -> {
            Task selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a task from the table first.");
                return;
            }
            dataManager.deleteTask(selected);
            table.setItems(FXCollections.observableArrayList(dataManager.getAllTasks()));
            showAlert("Success", "Task deleted.");
            updateSummaryInfo();
        });

        // When clicking on a row, load the fields
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
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

        formBox.getChildren().addAll(new Label("Manage Task"), txtTitle, txtDesc, cmbCategory, cmbPriority, dpDeadline, cmbStatus,
                                     new HBox(10, btnAdd, btnUpdate, btnDelete));

        pane.setCenter(table);
        pane.setRight(formBox);

        return pane;
    }

    // ---------------------------------------------------------------
    // TAB 2: CATEGORIES
    // ---------------------------------------------------------------
    private Pane createCategoriesPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        ListView<Category> listView = new ListView<>();
        ObservableList<Category> catObs = FXCollections.observableArrayList(dataManager.getAllCategories());
        listView.setItems(catObs);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName());
            }
        });

        // Form for create/rename
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(5));

        TextField txtCategoryName = new TextField();
        txtCategoryName.setPromptText("New category name");

        Button btnAdd = new Button("Add Category");
        btnAdd.setOnAction(e -> {
            if (txtCategoryName.getText().isEmpty()) return;
            dataManager.createCategory(txtCategoryName.getText());
            catObs.setAll(dataManager.getAllCategories());
            txtCategoryName.clear();
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
            catObs.setAll(dataManager.getAllCategories());
            txtCategoryName.clear();
        });

        Button btnDelete = new Button("Delete Category");
        btnDelete.setOnAction(e -> {
            Category selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a category first.");
                return;
            }
            dataManager.deleteCategory(selected);
            catObs.setAll(dataManager.getAllCategories());
            showAlert("Success", "Category and related tasks removed!");
            updateSummaryInfo();
        });

        formBox.getChildren().addAll(new Label("Category Name:"), txtCategoryName,
                new HBox(10, btnAdd, btnRename, btnDelete));

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

        ListView<Priority> listView = new ListView<>();
        ObservableList<Priority> prioObs = FXCollections.observableArrayList(dataManager.getAllPriorities());
        listView.setItems(prioObs);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Priority item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName());
            }
        });

        // Form
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(5));

        TextField txtPrioName = new TextField();
        txtPrioName.setPromptText("Priority name");

        Button btnAdd = new Button("Add Priority");
        btnAdd.setOnAction(e -> {
            if (txtPrioName.getText().isEmpty()) return;
            dataManager.createPriority(txtPrioName.getText());
            prioObs.setAll(dataManager.getAllPriorities());
            txtPrioName.clear();
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
            prioObs.setAll(dataManager.getAllPriorities());
            txtPrioName.clear();
            updateSummaryInfo();
        });

        Button btnDelete = new Button("Delete Priority");
        btnDelete.setOnAction(e -> {
            Priority selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a priority first.");
                return;
            }
            dataManager.deletePriority(selected);
            prioObs.setAll(dataManager.getAllPriorities());
            showAlert("Success", "Priority deleted or replaced with Default in tasks.");
            updateSummaryInfo();
        });

        formBox.getChildren().addAll(new Label("Priority:"), txtPrioName,
                new HBox(10, btnAdd, btnRename, btnDelete));

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

        TableColumn<Reminder, String> colTaskId = new TableColumn<>("Task Title");
        colTaskId.setCellValueFactory(cell -> {
            String taskId = cell.getValue().getTaskId();
            Task t = dataManager.getAllTasks().stream()
                    .filter(x -> x.getId().equals(taskId))
                    .findFirst().orElse(null);
            if (t != null) {
                return new javafx.beans.property.SimpleStringProperty(t.getTitle());
            } else {
                return new javafx.beans.property.SimpleStringProperty("N/A");
            }
        });

        TableColumn<Reminder, ReminderType> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Reminder, String> colDate = new TableColumn<>("Reminder Date");
        colDate.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getReminderDate();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "");
        });

        table.getColumns().addAll(colTaskId, colType, colDate);

        table.setItems(FXCollections.observableArrayList(dataManager.getAllReminders()));

        // Delete button
        Button btnDeleteReminder = new Button("Delete Reminder");
        btnDeleteReminder.setOnAction(e -> {
            Reminder selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Warning", "Select a reminder first.");
                return;
            }
            dataManager.deleteReminder(selected);
            table.setItems(FXCollections.observableArrayList(dataManager.getAllReminders()));
        });

        VBox vbox = new VBox(10, table, btnDeleteReminder);
        vbox.setPadding(new Insets(5));
        pane.setCenter(vbox);

        return pane;
    }

    // ---------------------------------------------------------------
    // TAB 5: SEARCH
    // ---------------------------------------------------------------
    private Pane createSearchPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Search by title (partial)");

        ComboBox<Category> cmbCategory = new ComboBox<>(FXCollections.observableArrayList(dataManager.getAllCategories()));
        cmbCategory.setPromptText("Category (optional)");
        cmbCategory.setConverter(ConverterUtils.getCategoryConverter());

        ComboBox<Priority> cmbPriority = new ComboBox<>(FXCollections.observableArrayList(dataManager.getAllPriorities()));
        cmbPriority.setPromptText("Priority (optional)");
        cmbPriority.setConverter(ConverterUtils.getPriorityConverter());

        Button btnSearch = new Button("Search");

        TableView<Task> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Task, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Task, String> colPrio = new TableColumn<>("Priority");
        colPrio.setCellValueFactory(cell -> {
            String pid = cell.getValue().getPriorityId();
            Priority p = dataManager.findPriorityById(pid);
            return new javafx.beans.property.SimpleStringProperty(p != null ? p.getName() : "??");
        });
        TableColumn<Task, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(cell -> {
            String cid = cell.getValue().getCategoryId();
            Category c = dataManager.findCategoryById(cid);
            return new javafx.beans.property.SimpleStringProperty(c != null ? c.getName() : "??");
        });
        TableColumn<Task, String> colDeadline = new TableColumn<>("Deadline");
        colDeadline.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getDeadline();
            return new javafx.beans.property.SimpleStringProperty(d != null ? d.toString() : "");
        });

        table.getColumns().addAll(colTitle, colPrio, colCat, colDeadline);

        btnSearch.setOnAction(e -> {
            Category cat = cmbCategory.getValue();
            Priority prio = cmbPriority.getValue();
            String titleFilter = txtTitle.getText();

            List<Task> results = dataManager.searchTasks(titleFilter, cat, prio);
            table.setItems(FXCollections.observableArrayList(results));
        });

        box.getChildren().addAll(new Label("Search Criteria:"),
                txtTitle, cmbCategory, cmbPriority, btnSearch,
                new Label("Results:"),
                table);

        return box;
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

    public static void main(String[] args) {
        launch(args);
    }
}

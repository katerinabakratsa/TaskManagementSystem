package com.taskmanagementsystem;

import java.util.ArrayList;
import java.util.List;

public class ReminderManager {
    private List<Reminder> reminders;

    public ReminderManager() {
        this.reminders = new ArrayList<>();
    }

    // Προσθήκη υπενθύμισης
    public void addReminder(Reminder reminder) {
        reminders.add(reminder);
    }

    // Διαγραφή υπενθύμισης
    public void deleteReminder(Reminder reminder) {
        reminders.remove(reminder);
    }

    // Διαγραφή όλων των υπενθυμίσεων που σχετίζονται με μια εργασία
    public void deleteRemindersForTask(Task task) {
        reminders.removeIf(reminder -> reminder.getTask().equals(task));
    }

    // Επιστροφή όλων των υπενθυμίσεων
    public List<Reminder> getAllReminders() {
        return reminders;
    }

    // Επιστροφή των υπενθυμίσεων για μια συγκεκριμένη εργασία
    public List<Reminder> getRemindersForTask(Task task) {
        List<Reminder> taskReminders = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (reminder.getTask().equals(task)) {
                taskReminders.add(reminder);
            }
        }
        return taskReminders;
    }
}

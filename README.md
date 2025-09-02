# Task Management System (MediaLab Assistant)

This is a task management application developed as part of a project for the Multimedia Technology course. The application allows the user to create, edit, and monitor multiple tasks, setting priorities, deadlines, and reminders to stay organized at all times. ✨

---

## Overview

**MediaLab Assistant** aims to help you effectively organize your tasks, providing a user-friendly graphical interface and features for:

- Creating, editing, and deleting tasks.
- Managing multiple categories.
- Setting and modifying priority levels.
- Defining (and automatically handling) reminders.
- Easily searching for tasks based on multiple criteria.

> All data is stored in JSON files located in a folder named `medialab`, allowing safe and easy transfer/modification of information.


---


## Technologies Used
- **Java 8+**: Main programming language.
- **JavaFX**: For the graphical user interface (GUI).
- **JSON αρχεία**: For storing and retrieving data in text format.
- **Javadoc**: For documenting public methods in specific classes.

---

## Installation & Setup Instructions 🔧
1. **Java 21 or newer** installed on your system.
2. **JavaFX** (may require separate installation depending on your Java version).
3. **Maven** (for compilation and execution).
4. Download or clone the repository from GitHub.

---

## User Guide 📝

### Launching the Application
- Run the `Main` class or the respective class containing the `main()` method.

### Main Screen
- At the top, you will see a summary (number of tasks, how many have been completed, etc.).
- The main area includes tabs/forms/buttons for:
  - **Task Management** (add, edit, delete)
  - **Category Management** (add, rename, delete) 
  - **Priority Management** (add, rename, delete)
  - **Reminder Management** (add, edit, delete)
  - **Task Search** (by title, category, or priority)
    
- On startup, if there are tasks marked as `Delayed`, a popup will appear informing you of the number of overdue tasks.

### Exiting the Application
- When closing the application, the current state is automatically saved into the corresponding JSON files inside the `medialab` folder.

---

---

**Ευχαριστούμε που δοκιμάσατε το MediaLab Assistant!**  
Καλή οργάνωση εργασιών! ✅

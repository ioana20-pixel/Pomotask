package com.example.pomodoroapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView greetingText;
    private FirebaseUser currentUser;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Button btnCreateTask, btnAccount;
    private LinearLayout taskContainer;
    private TextView tvNoTasks; // For "no tasks" message

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        greetingText = findViewById(R.id.tvGreeting);
        Button btnDateTime = findViewById(R.id.btnDateTime);
        btnCreateTask = findViewById(R.id.btnAddTask);
        ImageButton btnAccount = findViewById(R.id.btnAccount); // Find the Account button
        taskContainer = findViewById(R.id.taskContainer);

        // Pomodoro Timer: open TimerActivity
        CardView cardTimer = findViewById(R.id.cardTimer);
        cardTimer.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TimerActivity.class);
            startActivity(intent);
        });

        // Create a TextView to show "No tasks" message dynamically
        tvNoTasks = new TextView(this);
        tvNoTasks.setText("No tasks for today!");
        tvNoTasks.setTextColor(getResources().getColor(R.color.cream));
        tvNoTasks.setTextSize(16);
        tvNoTasks.setPadding(0, 20, 0, 0);
        tvNoTasks.setVisibility(View.GONE);
        taskContainer.addView(tvNoTasks);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        auth.addAuthStateListener(firebaseAuth -> {
            currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                String email = currentUser.getEmail();
                String username = email.split("@")[0];
                greetingText.setText("Welcome back, " + capitalize(username));
                loadTasksForToday();
            } else {
                greetingText.setText("Please sign in.");
                taskContainer.removeAllViews();
                taskContainer.addView(tvNoTasks);
                tvNoTasks.setVisibility(View.VISIBLE);
            }
        });

        // Display current date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Date now = new Date();
        String dateTimeText = dateFormat.format(now) + " - " + timeFormat.format(now);
        btnDateTime.setText(dateTimeText);

        btnDateTime.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        btnCreateTask.setOnClickListener(v -> showCreateTaskDialog());

        // Set OnClickListener for the Account Button to navigate to MyAccountActivity
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MyAccountActivity.class); // Intent to navigate to MyAccountActivity
            startActivity(intent); // Start MyAccountActivity
        });

        Button btnDashboard = findViewById(R.id.btnDashboard);
        btnDashboard.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, DashboardActivity.class));
        });
    }

    private void loadTasksForToday() {
        if (currentUser == null) return;

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Clear previous tasks (including noTasks message)
        taskContainer.removeAllViews();
        tvNoTasks.setVisibility(View.GONE);

        db.collection("tasks")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("date", todayDate)
                .orderBy("task") // optional ordering
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Show no tasks message
                        tvNoTasks.setVisibility(View.VISIBLE);
                        taskContainer.addView(tvNoTasks);
                        return;
                    }
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            addTaskCard(task, doc.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addTaskCard(Task task, String docId) {
        View taskCard = LayoutInflater.from(this).inflate(R.layout.item_task, taskContainer, false);

        TextView tvTaskTitle = taskCard.findViewById(R.id.tvTaskTitle);
        CheckBox cbComplete = taskCard.findViewById(R.id.cbComplete);
        ImageButton btnEditTask = taskCard.findViewById(R.id.btnEditTask);
        ImageButton btnDeleteTask = taskCard.findViewById(R.id.btnDeleteTask);

        tvTaskTitle.setText(task.task);
        cbComplete.setChecked(task.isComplete);

        // Update completion status in Firestore
        cbComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("tasks").document(docId)
                    .update("isComplete", isChecked)
                    .addOnFailureListener(e -> Toast.makeText(this, "Error updating task status", Toast.LENGTH_SHORT).show());
        });

        btnEditTask.setOnClickListener(v -> showEditTaskDialog(task, docId));

        btnDeleteTask.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.collection("tasks").document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                                    loadTasksForToday();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        taskContainer.addView(taskCard);
    }

    private void showEditTaskDialog(Task task, String docId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_create_task, null);

        EditText etTaskName = view.findViewById(R.id.etTaskName);
        TextView tvDate = view.findViewById(R.id.tvTaskDate);
        Button btnPickDate = view.findViewById(R.id.btnPickDate);

        etTaskName.setText(task.task);

        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar selectedDate = Calendar.getInstance();

        try {
            Date parsedDate = dbFormat.parse(task.date);
            if (parsedDate != null) {
                selectedDate.setTime(parsedDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvDate.setText(task.date);

        btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                String formatted = dbFormat.format(selectedDate.getTime());
                tvDate.setText(formatted);
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTaskName = etTaskName.getText().toString().trim();
                    String newDate = tvDate.getText().toString();

                    if (newTaskName.isEmpty()) {
                        Toast.makeText(this, "Task name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("tasks").document(docId)
                            .update("task", newTaskName, "date", newDate)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
                                loadTasksForToday();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateTaskDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.dialog_create_task, null);

        EditText etTaskName = view.findViewById(R.id.etTaskName);
        TextView tvDate = view.findViewById(R.id.tvTaskDate);
        Button btnPickDate = view.findViewById(R.id.btnPickDate);

        Calendar selectedDate = Calendar.getInstance();
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvDate.setText(dbFormat.format(selectedDate.getTime()));

        btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                tvDate.setText(dbFormat.format(selectedDate.getTime()));
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Create New Task")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String taskName = etTaskName.getText().toString().trim();
                    if (taskName.isEmpty()) {
                        Toast.makeText(this, "Task name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveTaskToFirestore(taskName, tvDate.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveTaskToFirestore(String taskName, String date) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated. Please sign in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = user.getUid();

                Task newTask = new Task(taskName, date, userId, false);
                db.collection("tasks").add(newTask)
                        .addOnSuccessListener(doc -> {
                            Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
                            loadTasksForToday();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Failed to refresh authentication. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String capitalize(String name) {
        if (name.isEmpty()) return "";
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static class Task {
        public String task;
        public String date;
        public String userId;
        public boolean isComplete;  // Added completion flag

        public Task() {}  // Firestore requires no-arg constructor

        public Task(String task, String date, String userId, boolean isComplete) {
            this.task = task;
            this.date = date;
            this.userId = userId;
            this.isComplete = isComplete;
        }
    }
}

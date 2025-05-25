package com.example.pomodoroapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarActivity extends AppCompatActivity {

    private GridView gridCalendar;
    private ImageButton btnBack, btnPrevMonth, btnNextMonth;
    private TextView tvMonthYear;
    private Calendar currentCalendar;
    private Calendar todayCalendar;
    private String selectedDate;
    private SimpleDateFormat dateFormat, monthYearFormat;
    private List<String> daysWithTasks = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        btnBack = findViewById(R.id.btnBack);
        gridCalendar = findViewById(R.id.gridCalendar);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvMonthYear = findViewById(R.id.tvMonthYear);

        currentCalendar = Calendar.getInstance();
        todayCalendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        selectedDate = null;

        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

//        Toast.makeText(this, "Current User ID: " + currentUserId, Toast.LENGTH_SHORT).show();

        btnBack.setOnClickListener(v -> finish());
        btnPrevMonth.setOnClickListener(v -> changeMonth(-1));
        btnNextMonth.setOnClickListener(v -> changeMonth(1));

        loadTaskDates();
    }


    private void changeMonth(int offset) {
        gridCalendar.animate().alpha(0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentCalendar.add(Calendar.MONTH, offset);
                loadTaskDates(); // Reload tasks for new month
                gridCalendar.animate().alpha(1f).setDuration(200).setListener(null);
            }
        });
    }

    private void loadTaskDates() {
        Calendar firstDay = (Calendar) currentCalendar.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        Calendar lastDay = (Calendar) currentCalendar.clone();
        lastDay.set(Calendar.DAY_OF_MONTH, currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        String startDate = dateFormat.format(firstDay.getTime());
        String endDate = dateFormat.format(lastDay.getTime());

        db.collection("tasks")
                .whereEqualTo("userId", currentUserId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener(snapshots -> {
                    Set<String> taskDates = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String date = doc.getString("date");
                        if (date != null) taskDates.add(date);
                    }
                    daysWithTasks = new ArrayList<>(taskDates);
                    updateCalendar();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateCalendar() {
        tvMonthYear.setText(monthYearFormat.format(currentCalendar.getTime()));
        gridCalendar.setAdapter(new CalendarAdapter());
    }

    private class CalendarAdapter extends BaseAdapter {
        private final int daysInMonth;
        private final int firstDayOfWeek;

        public CalendarAdapter() {
            Calendar tempCal = (Calendar) currentCalendar.clone();
            tempCal.set(Calendar.DAY_OF_MONTH, 1);
            firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
            daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        @Override
        public int getCount() {
            return daysInMonth + firstDayOfWeek;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CalendarActivity.this)
                        .inflate(R.layout.calendar_day_item, parent, false);
            }

            TextView tvDay = convertView.findViewById(R.id.tvDayNumber);
            View highlightCircle = convertView.findViewById(R.id.highlightCircle);

            if (position < firstDayOfWeek) {
                tvDay.setText("");
                highlightCircle.setVisibility(View.GONE);
                convertView.setClickable(false);
                convertView.setBackground(null);
            } else {
                int dayNumber = position - firstDayOfWeek + 1;
                tvDay.setText(String.valueOf(dayNumber));
                convertView.setClickable(true);

                Calendar dayCal = (Calendar) currentCalendar.clone();
                dayCal.set(Calendar.DAY_OF_MONTH, dayNumber);
                String dayString = dateFormat.format(dayCal.getTime());

                // Day has tasks?
                if (daysWithTasks.contains(dayString)) {
                    highlightCircle.setVisibility(View.VISIBLE);
                } else {
                    highlightCircle.setVisibility(View.GONE);
                }

                // Today
                if (isSameDate(dayCal, todayCalendar)) {
                    tvDay.setBackgroundResource(R.drawable.circle_today_background);
                    tvDay.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    tvDay.setBackground(null);
                    tvDay.setTextColor(getResources().getColor(android.R.color.white));
                }

                // Selected
                if (dayString.equals(selectedDate)) {
                    tvDay.setBackgroundResource(R.drawable.circle_selected_background);
                    tvDay.setTextColor(getResources().getColor(android.R.color.black));
                }

                convertView.setOnClickListener(v -> {
                    selectedDate = dayString;
                    showTasksDialog(dayString);
                    notifyDataSetChanged();
                });
            }

            return convertView;
        }
    }

    private boolean isSameDate(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private void showTasksDialog(String date) {
        db.collection("tasks")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<String> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        String task = doc.getString("task");
                        if (task != null) tasks.add(task);
                    }
                    displayTasksDialog(date, tasks);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void displayTasksDialog(String date, List<String> tasks) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tasks for " + date);

        if (tasks.isEmpty()) {
            builder.setMessage("No tasks for this day.");
        } else {
            StringBuilder message = new StringBuilder();
            for (String task : tasks) {
                message.append("• ").append(task).append("\n");
            }
            builder.setMessage(message.toString());
        }

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}

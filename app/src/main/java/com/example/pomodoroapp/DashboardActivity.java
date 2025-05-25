package com.example.pomodoroapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvTodayStats, tvProgressTitle, tvHistoryTitle, tvAchievementsTitle;
    private ProgressBar progressBarToday;
    private RecyclerView rvSessions, rvAchievements;

    private List<Session> sessionList = new ArrayList<>();
    private List<Achievement> achievementList = new ArrayList<>();
    private SessionAdapter sessionAdapter;
    private AchievementAdapter achievementAdapter;

    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private int pomodorosToday = 0;
    private int pomodoroGoal = 8; // Arbitrary goal for the progress bar
    private int streak = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvTodayStats = findViewById(R.id.tvTodayStats);
        progressBarToday = findViewById(R.id.progressBarToday);
        rvSessions = findViewById(R.id.rvSessions);
        rvAchievements = findViewById(R.id.rvAchievements);

        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        sessionAdapter = new SessionAdapter(sessionList);
        rvSessions.setAdapter(sessionAdapter);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvAchievements.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        achievementAdapter = new AchievementAdapter(achievementList);
        rvAchievements.setAdapter(achievementAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        loadSessionsForToday();
        loadAchievements();
        loadStreak();
    }

    // GROUPING logic: 1 session = 4 pomodoros
    private void loadSessionsForToday() {
        if (currentUser == null) return;
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("users")
                .document(currentUser.getUid())
                .collection("sessions")
                .whereEqualTo("date", todayDate)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Session> allWorkSessions = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Session session = doc.toObject(Session.class);
                        if (session != null && "Work".equals(session.type)) {
                            allWorkSessions.add(session);
                        }
                    }
                    sessionList.clear();
                    pomodorosToday = allWorkSessions.size();

                    int setNum = 1;
                    for (int i = 0; i + 3 < allWorkSessions.size(); i += 4) {
                        Session s = new Session();
                        s.type = "Pomodoro Set";
                        s.sessionNum = setNum++;
                        s.date = allWorkSessions.get(i).date;

                        // Optional: Show start and end time of set
                        String start = allWorkSessions.get(i).startTime;
                        String end = allWorkSessions.get(i + 3).startTime;
                        s.startTime = start + " - " + end;

                        // Sum durations for the set
                        int totalDuration = 0;
                        for (int j = i; j < i + 4; j++) {
                            totalDuration += allWorkSessions.get(j).duration;
                        }
                        s.duration = totalDuration;

                        sessionList.add(s);
                    }
                    sessionAdapter.notifyDataSetChanged();
                    updateProgress();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load sessions.", Toast.LENGTH_SHORT).show());
    }

    private void updateProgress() {
        progressBarToday.setMax(pomodoroGoal);
        progressBarToday.setProgress(pomodorosToday);
        tvTodayStats.setText(pomodorosToday + "/" + pomodoroGoal + " Pomodoros • Streak: " + streak + " days");
    }

    private void loadAchievements() {
        if (currentUser == null) return;
        db.collection("users")
                .document(currentUser.getUid())
                .collection("achievements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    achievementList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Achievement ach = doc.toObject(Achievement.class);
                        if (ach != null) achievementList.add(ach);
                    }
                    achievementAdapter.notifyDataSetChanged();
                });
    }

    private void loadStreak() {
        if (currentUser == null) return;
        db.collection("users")
                .document(currentUser.getUid())
                .collection("sessions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> daysWithSessions = new HashSet<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Session session = doc.toObject(Session.class);
                        if (session != null && session.date != null)
                            daysWithSessions.add(session.date);
                    }
                    streak = 0;
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    while (daysWithSessions.contains(df.format(cal.getTime()))) {
                        streak++;
                        cal.add(Calendar.DAY_OF_YEAR, -1);
                    }
                    updateProgress();
                });
    }
}

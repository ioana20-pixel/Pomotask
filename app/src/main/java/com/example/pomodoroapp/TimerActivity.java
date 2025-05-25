package com.example.pomodoroapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TimerActivity extends AppCompatActivity {

    private static final int WORK_DURATION = 1 * 3;       // 25 minutes
    private static final int SHORT_BREAK_DURATION = 1 * 2; // 5 minutes
    private static final int LONG_BREAK_DURATION = 1 * 2; // 15 minutes
    private static final int POMODOROS_BEFORE_LONG_BREAK = 4;

    // UI
    private TextView tvTimer, tvState, tvSessionInfo;
    private ImageButton btnStartPause, btnReset, btnBack;
    private CircularProgressIndicator progressIndicator;
    private LinearLayout layoutPomodoros;

    // State
    private CountDownTimer countDownTimer;
    private int timeLeftInSeconds = WORK_DURATION;
    private boolean isRunning = false;
    private int pomodoroCount = 0;
    private int sessionCount = 1;
    private String currentState = "Work"; // "Work", "Short Break", "Long Break"

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // For achievements
    private int pomodorosToday = 0;
    private int totalPomodoros = 0;
    private int streak = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        // Bind UI elements
        tvTimer = findViewById(R.id.tvTimer);
        tvState = findViewById(R.id.tvState);
        tvSessionInfo = findViewById(R.id.tvSessionInfo);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack);
        progressIndicator = findViewById(R.id.progressIndicator);
        layoutPomodoros = findViewById(R.id.layoutPomodoros);

        // Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        updateAllUI();
        loadStatsForAchievements();

        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());
        btnBack.setOnClickListener(v -> finish());

        Button btnDashboard = findViewById(R.id.btnDashboard);
        btnDashboard.setOnClickListener(v -> {
            startActivity(new Intent(TimerActivity.this, DashboardActivity.class));
        });
    }

    private void startTimer() {
        isRunning = true;
        btnStartPause.setImageResource(R.drawable.ic_pause);

        countDownTimer = new CountDownTimer(timeLeftInSeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInSeconds = (int) (millisUntilFinished / 1000);
                updateTimerUI();
            }
            @Override
            public void onFinish() {
                handleSessionEnd();
            }
        }.start();
    }

    private void pauseTimer() {
        isRunning = false;
        btnStartPause.setImageResource(R.drawable.ic_play);
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void resetTimer() {
        pauseTimer();
        switch (currentState) {
            case "Work":        timeLeftInSeconds = WORK_DURATION; break;
            case "Short Break": timeLeftInSeconds = SHORT_BREAK_DURATION; break;
            case "Long Break":  timeLeftInSeconds = LONG_BREAK_DURATION; break;
        }
        updateTimerUI();
    }

    private void handleSessionEnd() {
        int sessionDuration = getMaxForCurrentState();
        recordSessionInFirestore(currentState, sessionDuration);

        if ("Work".equals(currentState)) {
            pomodoroCount++;
            if (pomodoroCount % POMODOROS_BEFORE_LONG_BREAK == 0) {
                currentState = "Long Break";
                timeLeftInSeconds = LONG_BREAK_DURATION;
            } else {
                currentState = "Short Break";
                timeLeftInSeconds = SHORT_BREAK_DURATION;
            }
        } else {
            if ("Long Break".equals(currentState)) {
                sessionCount++;
                pomodoroCount = 0;
            }
            currentState = "Work";
            timeLeftInSeconds = WORK_DURATION;
        }
        updateAllUI();
        isRunning = false;
        btnStartPause.setImageResource(R.drawable.ic_play);

        // ** After session ends, refresh stats & check achievements **
        loadStatsForAchievements();
    }

    private void recordSessionInFirestore(String sessionType, int durationInSeconds) {
        if (currentUser == null) return;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String startTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> session = new HashMap<>();
        session.put("userId", currentUser.getUid());
        session.put("type", sessionType);
        session.put("duration", durationInSeconds / 60); // store as minutes
        session.put("date", date);
        session.put("startTime", startTime);
        session.put("sessionNum", sessionCount);

        db.collection("users").document(currentUser.getUid())
                .collection("sessions")
                .add(session);
    }

    private void loadStatsForAchievements() {
        if (currentUser == null) return;
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Pomodoros Today & Total Pomodoros
        db.collection("users").document(currentUser.getUid())
                .collection("sessions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pomodorosToday = 0;
                    totalPomodoros = 0;
                    List<String> daysWithPomodoro = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Session s = doc.toObject(Session.class);
                        if (s != null && "Work".equals(s.type)) {
                            totalPomodoros++;
                            if (todayDate.equals(s.date)) pomodorosToday++;
                            if (!daysWithPomodoro.contains(s.date)) daysWithPomodoro.add(s.date);
                        }
                    }
                    checkAndUnlockAchievements();
                });

        // 2. Streak (how many days in a row with sessions)
        db.collection("users").document(currentUser.getUid())
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
                    checkAndUnlockAchievements();
                });
    }

    private void checkAndUnlockAchievements() {
        // Unlock at the end of every session, based on updated stats!
        // 1. First Pomodoro
        if (totalPomodoros == 1) {
            unlockAchievementIfNeeded("first_pomodoro", "First Pomodoro", "Completed your first work session!");
        }
        // 2. First Full Set (4 Pomodoros)
        if (totalPomodoros == 4) {
            unlockAchievementIfNeeded("pomodoro_set", "First Set", "Completed a set of 4 Pomodoros!");
        }
        // 3. Daily Goal (8 Pomodoros in a day)
        if (pomodorosToday >= 8) {
            unlockAchievementIfNeeded("daily_goal", "Daily Goal", "Completed 8 Pomodoros in a day!");
        }
        // 4. Streaks
        if (streak == 3) {
            unlockAchievementIfNeeded("3_day_streak", "3-Day Streak", "Used Pomodoro for 3 days in a row!");
        }
        if (streak == 7) {
            unlockAchievementIfNeeded("7_day_streak", "7-Day Streak", "Used Pomodoro for a week straight!");
        }
        // 5. Total Pomodoros (Lifetime)
        if (totalPomodoros == 10) {
            unlockAchievementIfNeeded("pomodoro_10", "Pomodoro Novice", "Completed 10 Pomodoros in total!");
        }
        if (totalPomodoros == 50) {
            unlockAchievementIfNeeded("pomodoro_50", "Pomodoro Pro", "Completed 50 Pomodoros in total!");
        }
        if (totalPomodoros == 100) {
            unlockAchievementIfNeeded("pomodoro_100", "Pomodoro Master", "Completed 100 Pomodoros in total!");
        }
    }

    private void unlockAchievementIfNeeded(String achievementId, String name, String desc) {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).collection("achievements")
                .document(achievementId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || !Boolean.TRUE.equals(doc.getBoolean("unlocked"))) {
                        Achievement ach = new Achievement(achievementId, name, desc, true, System.currentTimeMillis());
                        db.collection("users").document(currentUser.getUid())
                                .collection("achievements").document(achievementId).set(ach);
                        Toast.makeText(this, "Achievement Unlocked: " + name, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Shows a tomato icon for each completed pomodoro (up to 4 per session)
    private void updatePomodoroIcons() {
        layoutPomodoros.removeAllViews();
        for (int i = 0; i < pomodoroCount % POMODOROS_BEFORE_LONG_BREAK; i++) {
            ImageView tomato = new ImageView(this);
            tomato.setImageResource(R.drawable.ic_tomato);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(64, 64);
            params.setMargins(6, 0, 6, 0);
            tomato.setLayoutParams(params);
            layoutPomodoros.addView(tomato);
        }
    }

    private void updateAllUI() {
        updateTimerUI();
        tvState.setText(currentState);
        tvSessionInfo.setText("Session: " + sessionCount + "   •   Pomodoros: " + pomodoroCount);
        updatePomodoroIcons();
    }

    private void updateTimerUI() {
        int minutes = timeLeftInSeconds / 60;
        int seconds = timeLeftInSeconds % 60;
        tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
        int max = getMaxForCurrentState();
        int progress = (int) (((double) timeLeftInSeconds / max) * 100);
        progressIndicator.setProgress(progress);
    }

    private int getMaxForCurrentState() {
        switch (currentState) {
            case "Work":        return WORK_DURATION;
            case "Short Break": return SHORT_BREAK_DURATION;
            case "Long Break":  return LONG_BREAK_DURATION;
        }
        return WORK_DURATION;
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseTimer();
    }
}

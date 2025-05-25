package com.example.pomodoroapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TimerActivity extends AppCompatActivity {

    private static final int WORK_DURATION = 25 * 60;       // 25 minutes
    private static final int SHORT_BREAK_DURATION = 5 * 60; // 5 minutes
    private static final int LONG_BREAK_DURATION = 15 * 60; // 15 minutes
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
        btnStartPause.setImageResource(R.drawable.ic_pause); // swap to pause icon

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
        btnStartPause.setImageResource(R.drawable.ic_play); // swap to play icon
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
        // 1. Save session to Firestore
        int sessionDuration = getMaxForCurrentState();
        recordSessionInFirestore(currentState, sessionDuration);

        // 2. Move to next state/session
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
    }

    private void recordSessionInFirestore(String sessionType, int durationInSeconds) {
        if (currentUser == null) return;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String startTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> session = new HashMap<>();
        session.put("userId", currentUser.getUid());
        session.put("type", sessionType);
        session.put("duration", durationInSeconds);
        session.put("date", date);
        session.put("startTime", startTime);
        session.put("sessionNum", sessionCount);

        db.collection("sessions").add(session)
                .addOnSuccessListener(documentReference -> {
                    // Optional: Log or toast
                    // Toast.makeText(this, "Session recorded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Optional: Log or toast
                    // Toast.makeText(this, "Failed to record session", Toast.LENGTH_SHORT).show();
                });
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

    // Shows a tomato icon for each completed pomodoro (up to 4 per session)
    private void updatePomodoroIcons() {
        layoutPomodoros.removeAllViews();
        for (int i = 0; i < pomodoroCount % POMODOROS_BEFORE_LONG_BREAK; i++) {
            ImageView tomato = new ImageView(this);
            tomato.setImageResource(R.drawable.ic_tomato); // Use your themed icon!
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(64, 64);
            params.setMargins(6, 0, 6, 0);
            tomato.setLayoutParams(params);
            layoutPomodoros.addView(tomato);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseTimer();
    }
}

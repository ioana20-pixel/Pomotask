package com.example.pomodoroapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardActivity extends AppCompatActivity {

    private RecyclerView rvSessions;
    private SessionAdapter adapter;
    private List<Session> sessionList = new ArrayList<>();
    private TextView tvNoSessions;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        rvSessions = findViewById(R.id.rvSessions);
        tvNoSessions = findViewById(R.id.tvNoSessions);

        adapter = new SessionAdapter(sessionList);
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        rvSessions.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        fetchSessionsForToday();
    }

    private void fetchSessionsForToday() {
        if (user == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("sessions")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("date", today)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    sessionList.clear();
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Session session = doc.toObject(Session.class);
                            sessionList.add(session);
                        }
                        adapter.notifyDataSetChanged();
                        tvNoSessions.setVisibility(View.GONE);
                    } else {
                        tvNoSessions.setVisibility(View.VISIBLE);
                    }
                });
    }
}

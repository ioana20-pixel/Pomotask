package com.example.pomodoroapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 1500; // 1.5 seconds
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = auth.getCurrentUser();
            Intent nextIntent;

            if (currentUser != null) {
                // User is logged in -> go to HomeActivity
                nextIntent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                // User not logged in -> go to LoginActivity
                nextIntent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(nextIntent);
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}

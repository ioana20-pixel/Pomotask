package com.example.pomodoroapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // ADD THIS LINE

        FirebaseApp.initializeApp(this);

        db = FirebaseFirestore.getInstance();

        // Example: Writing a test document
        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "Firestore is working!");

        db.collection("test").add(testData)
                .addOnSuccessListener(documentReference -> Log.d("FIRESTORE", "DocumentSnapshot added"))
                .addOnFailureListener(e -> Log.w("FIRESTORE", "Error adding document", e));
    }
}
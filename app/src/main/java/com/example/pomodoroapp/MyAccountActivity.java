package com.example.pomodoroapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MyAccountActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private ImageView ivProfilePicture;
    private TextView tvUsername, tvEmail;
    private Button btnLogout, btnDeleteAccount, btnChangePassword;
    private TextView tvSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        tvSupport = findViewById(R.id.tvSupport);

        if (currentUser != null) {
            // Set profile information
            String username = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User";
            tvUsername.setText(username);
            tvEmail.setText(currentUser.getEmail());

            // Dynamically set the profile picture with user's initial letter
            setProfilePictureInitial(username);
        } else {
            Toast.makeText(this, "No user is currently logged in", Toast.LENGTH_SHORT).show();
        }

        // Logout Button
        btnLogout.setOnClickListener(v -> logoutUser());

        // Delete Account Button
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        // Change Password Button
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        Button btnDashboard = findViewById(R.id.btnDashboard);
        btnDashboard.setOnClickListener(v -> {
            startActivity(new Intent(MyAccountActivity.this, DashboardActivity.class));
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Support Section - You can implement this to open a contact form or email
        tvSupport.setOnClickListener(v -> {
            // For example: Open email app to contact support
            Toast.makeText(this, "Opening support contact...", Toast.LENGTH_SHORT).show();
        });
    }

    // Function to dynamically set user's initial letter in the profile picture
    private void setProfilePictureInitial(String username) {
        // Extract the first letter of the username or email as the initial
        String initial = username.substring(0, 1).toUpperCase(); // Get the first letter and capitalize it

        // Find the FrameLayout container
        FrameLayout profileContainer = findViewById(R.id.flProfileContainer); // The FrameLayout container in XML

        // Find the existing ImageView for the profile picture
        ImageView ivProfilePicture = findViewById(R.id.ivProfilePicture);

        // Create a TextView to display the initial letter
        TextView tvInitial = new TextView(this);
        tvInitial.setText(initial); // Set the initial letter
        tvInitial.setTextColor(getResources().getColor(R.color.white)); // White text color
        tvInitial.setTextSize(40f); // Font size for the initial
        tvInitial.setGravity(Gravity.CENTER); // Center the text in the circle
        tvInitial.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Make sure the FrameLayout is empty before adding the views
        profileContainer.removeAllViews();

        // Add the ImageView and TextView to the FrameLayout (with ImageView as the background and TextView on top)
        profileContainer.addView(ivProfilePicture); // Add the profile picture
        profileContainer.addView(tvInitial); // Add the initial text
    }


    // Logout Functionality
    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut(); // Log out from Firebase
                    Toast.makeText(MyAccountActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                    // Redirect to the login screen
                    Intent intent = new Intent(MyAccountActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear stack
                    startActivity(intent);
                    finish();  // Close MyAccountActivity so the user cannot go back to it
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    // Delete Account Functionality
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action is permanent.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteAccount();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (currentUser != null) {
            currentUser.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MyAccountActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            auth.signOut();
                            // Go to login screen and clear back stack
                            Intent intent = new Intent(MyAccountActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(MyAccountActivity.this, "Failed to delete account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    // Change Password Functionality
    private void showChangePasswordDialog() {
        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("Enter new password");
        newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("Please enter your new password")
                .setView(newPasswordInput)
                .setPositiveButton("Change", (dialog, which) -> {
                    String newPassword = newPasswordInput.getText().toString().trim();
                    if (newPassword.isEmpty()) {
                        Toast.makeText(MyAccountActivity.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updatePassword(newPassword);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePassword(String newPassword) {
        if (currentUser != null) {
            currentUser.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MyAccountActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MyAccountActivity.this, "Failed to update password: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}

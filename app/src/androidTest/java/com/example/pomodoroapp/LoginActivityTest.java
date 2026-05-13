package com.example.pomodoroapp;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.assertion.ViewAssertions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    // ── Vizibilitate UI ──

    @Test
    public void testEmailField_isDisplayed() {
        onView(withId(R.id.etEmail))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testPasswordField_isDisplayed() {
        onView(withId(R.id.etPassword))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginButton_isDisplayed() {
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testRegisterLink_isDisplayed() {
        onView(withId(R.id.tvRegisterLink))
                .check(matches(isDisplayed()));
    }

    // ── Validare câmpuri goale ──

    @Test
    public void testLogin_withEmptyFields_showsToast() {
        onView(withId(R.id.btnLogin))
                .perform(click());

        onView(withText("All fields are required"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLogin_withEmptyPassword_showsToast() {
        onView(withId(R.id.etEmail))
                .perform(typeText("test@test.com"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin))
                .perform(click());

        onView(withText("All fields are required"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLogin_withEmptyEmail_showsToast() {
        onView(withId(R.id.etPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin))
                .perform(click());

        onView(withText("All fields are required"))
                .check(matches(isDisplayed()));
    }

    // ── Input ──

    @Test
    public void testEmailField_acceptsInput() {
        onView(withId(R.id.etEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard())
                .check(matches(withText("test@example.com")));
    }

    @Test
    public void testPasswordField_acceptsInput() {
        onView(withId(R.id.etPassword))
                .perform(typeText("password123"), closeSoftKeyboard())
                .check(matches(withText("password123")));
    }

    @Test
    public void testEmailField_canBeCleared() {
        onView(withId(R.id.etEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard())
                .perform(clearText())
                .check(matches(withText("")));
    }

    // ── Navigare ──

    @Test
    public void testRegisterLink_click_opensRegisterActivity() {
        onView(withId(R.id.tvRegisterLink))
                .perform(click());

        // Verificăm că suntem pe ecranul de Register
        onView(withId(R.id.btnRegister))
                .check(matches(isDisplayed()));
    }
}
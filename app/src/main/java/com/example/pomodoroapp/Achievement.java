package com.example.pomodoroapp;

public class Achievement {
    public String id;
    public String name;
    public String description;
    public boolean unlocked;
    public long unlockedAt; // timestamp

    public Achievement() {}
    public Achievement(String id, String name, String description, boolean unlocked, long unlockedAt) {
        this.id = id; this.name = name; this.description = description;
        this.unlocked = unlocked; this.unlockedAt = unlockedAt;
    }
}
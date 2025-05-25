package com.example.pomodoroapp;

public class Session {
    public String type;
    public int duration;
    public String date;
    public String startTime;
    public int sessionNum;

    public Session() {} // Needed for Firestore

    public Session(String type, int duration, String date, String startTime, int sessionNum) {
        this.type = type;
        this.duration = duration;
        this.date = date;
        this.startTime = startTime;
        this.sessionNum = sessionNum;
    }
}

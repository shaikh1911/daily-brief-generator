package com.example.dailybrief.model;

public class DailyBrief {

    private String location;
    private String interest;
    private String content; // the ~200 word daily brief

    // Constructor
    public DailyBrief(String location, String interest, String content) {
        this.location = location;
        this.interest = interest;
        this.content = content;
    }

    // Getters and Setters
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

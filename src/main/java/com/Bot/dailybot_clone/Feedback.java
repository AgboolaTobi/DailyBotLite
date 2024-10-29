package com.Bot.dailybot_clone;

public class Feedback {
    private String teamMemberId;
    private String response;

    public Feedback(String teamMemberId, String response) {
        this.teamMemberId = teamMemberId;
        this.response = response;
    }

    public String getTeamMemberId() {
        return teamMemberId;
    }

    public String getResponse() {
        return response;
    }
}

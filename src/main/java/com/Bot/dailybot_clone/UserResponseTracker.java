package com.Bot.dailybot_clone;

import java.util.ArrayList;
import java.util.List;

public class UserResponseTracker {
    private int questionIndex = 0;
    private final List<String> responses = new ArrayList<>();
    private boolean isCompleted = false;
    private final String channelId;
    private boolean awaitingNextChannel = false;

    public UserResponseTracker(String channelId) {
        this.channelId = channelId;
    }

    public synchronized void recordResponse(String response) {
        if (!isCompleted) {
            while (responses.size() <= questionIndex) {
                responses.add("");
            }
            responses.set(questionIndex, response);
            questionIndex++;

            if (questionIndex >= DailyBot.getQuestions().length) {
                isCompleted = true;
                awaitingNextChannel = true;
            }
        }
    }

    public synchronized int getQuestionIndex() {
        return questionIndex;
    }

    public synchronized String getResponse(int index) {
        return index < responses.size() ? responses.get(index) : "";
    }

    public synchronized boolean isCompleted() {
        return isCompleted;
    }

    public String getChannelId() {
        return channelId;
    }

    public synchronized boolean isAwaitingNextChannel() {
        return awaitingNextChannel;
    }

    public synchronized void resetTrackerForNextChannel() {
        questionIndex = 0;
        responses.clear();
        isCompleted = false;
        awaitingNextChannel = false;
    }
}
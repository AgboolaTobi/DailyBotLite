package com.Bot.dailybot_clone;

import java.util.ArrayList;
import java.util.List;

public class UserResponseTracker {

    private int questionIndex = 0;
    private final List<String> responses = new ArrayList<>();
    private boolean isCompleted = false;
    private final String channelId;

    public UserResponseTracker(String channelId) {
        this.channelId = channelId;
    }

    public void recordResponse(String response) {
        if (!isCompleted) {
            while (responses.size() <= questionIndex) {
                responses.add("");
            }
            responses.set(questionIndex, response);
            questionIndex++;

            if (questionIndex >= 3) {
                isCompleted = true;
            }
        }
    }

    public int getQuestionIndex() {
        return questionIndex;
    }

    public String getResponse(int index) {
        return index < responses.size() ? responses.get(index) : "";
    }

    public boolean isCompleted() {
        return !isCompleted;
    }

    public String getChannelId() {
        return channelId;
    }
}

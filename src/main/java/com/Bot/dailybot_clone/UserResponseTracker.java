package com.Bot.dailybot_clone;

import java.util.ArrayList;
import java.util.List;

public class UserResponseTracker {
    private int questionIndex = 0;
    private final List<String> responses = new ArrayList<>();


    public void recordResponse(String response) {

        while (responses.size() <= questionIndex) {
            responses.add("");
        }
        responses.set(questionIndex, response);
        questionIndex++;
    }


    public int getQuestionIndex() {
        return questionIndex;
    }


    public String getResponse(int index) {
        return index < responses.size() ? responses.get(index) : "";
    }
}

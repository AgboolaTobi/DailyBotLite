package com.Bot.dailybot_clone;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.event.MessageEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyBot {
    private final Slack slack;
    private final String channelId;
    private final String slackToken;
    private final List<String> teamMembers;
    private final Map<String, UserResponseTracker> userResponses = new HashMap<>();

    private static final String[] QUESTIONS = {
            "What did you do yesterday?",
            "What are you doing today?",
            "Do you have any blockers?"
    };

    public DailyBot(String channelId, String slackToken) {
        this.channelId = channelId;
        this.slackToken = slackToken;
        this.slack = Slack.getInstance();
        this.teamMembers = List.of("U07TNGL0S74");
    }

    public void sendDailyQuestions() {
        for (String memberId : teamMembers) {
            userResponses.put(memberId, new UserResponseTracker());
            sendQuestion(memberId, 0);
        }
    }

    private void sendQuestion(String memberId, int questionIndex) {
        try {
            slack.methods(slackToken).chatPostMessage(ChatPostMessageRequest.builder()
                    .channel(memberId)
                    .text(QUESTIONS[questionIndex])
                    .build());
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }

    public void handleResponse(MessageEvent event) {
        String userId = event.getUser();
        String responseText = event.getText();
        UserResponseTracker tracker = userResponses.get(userId);


        if (tracker == null || responseText.isEmpty()) return;


        tracker.recordResponse(responseText);
        int nextQuestionIndex = tracker.getQuestionIndex();


        if (nextQuestionIndex < QUESTIONS.length) {
            sendQuestion(userId, nextQuestionIndex);
        } else {

            sendSummaryToChannel(userId, tracker);
            userResponses.remove(userId);
        }
    }

    private void sendSummaryToChannel(String userId, UserResponseTracker tracker) {
        StringBuilder summary = new StringBuilder("Daily Summary for <@").append(userId).append(">: \n");
        for (int i = 0; i < QUESTIONS.length; i++) {
            summary.append(QUESTIONS[i]).append(": ").append(tracker.getResponse(i)).append("\n");
        }

        try {
            slack.methods(slackToken).chatPostMessage(ChatPostMessageRequest.builder()
                    .channel(channelId)
                    .text(summary.toString())
                    .build());
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }
}

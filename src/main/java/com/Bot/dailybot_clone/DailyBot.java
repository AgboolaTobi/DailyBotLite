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
        this.teamMembers = List.of("U07TNGL0S74"); // Add the IDs of the team members here
    }

    public void sendDailyQuestions() {
        for (String memberId : teamMembers) {
            userResponses.put(memberId, new UserResponseTracker());
            sendQuestion(memberId); // Start with the first question
        }
    }

    private void sendQuestion(String memberId) {
        UserResponseTracker tracker = userResponses.get(memberId);

        // Only send the next question if the user has not completed the questions
        if (tracker != null && !tracker.isCompleted()) {
            int questionIndex = tracker.getQuestionIndex();

            // Only send the current question
            if (questionIndex < QUESTIONS.length) {
                try {
                    slack.methods(slackToken).chatPostMessage(ChatPostMessageRequest.builder()
                            .channel(memberId)
                            .text(QUESTIONS[questionIndex])
                            .build());
                } catch (IOException | SlackApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleResponse(MessageEvent event) {
        String userId = event.getUser();
        String responseText = event.getText();
        UserResponseTracker tracker = userResponses.get(userId);

        // If no tracker found for user or response is empty, return early
        if (tracker == null || responseText.isEmpty()) return;

        // Record the user's response
        tracker.recordResponse(responseText);

        // Check if the user has completed all questions
        if (!tracker.isCompleted()) {
            // Send the next question
            sendQuestion(userId);
        } else {
            // Send summary to the channel after all questions are answered
            sendSummaryToChannel(userId, tracker);
            userResponses.remove(userId); // Remove the user from tracking after summary is sent
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

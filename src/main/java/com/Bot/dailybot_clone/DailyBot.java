package com.Bot.dailybot_clone;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.conversations.ConversationsMembersResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.auth.AuthTestResponse;
import com.slack.api.model.User;
import com.slack.api.model.event.MessageEvent;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyBot {
    private final Slack slack;
    private final List<String> channelIds;
    private final String slackToken;
    private final String summaryChannelId;
    private final Map<String, UserResponseTracker> userResponses = new HashMap<>();
    private String botUserId;

    private static final String[] QUESTIONS = {
            "What did you do yesterday?",
            "What are you doing today?",
            "Do you have any blockers?"
    };

    public DailyBot(List<String> channelIds, String summaryChannelId, String slackToken) {
        this.channelIds = channelIds;
        this.summaryChannelId = summaryChannelId;
        this.slackToken = slackToken;
        this.slack = Slack.getInstance();


        try {
            AuthTestResponse authResponse = slack.methods(slackToken).authTest(req -> req);
            if (authResponse.isOk()) {
                botUserId = authResponse.getUserId();
            } else {
                System.err.println("Error fetching bot user ID: " + authResponse.getError());
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }

    public void sendDailyQuestionsToChannelMembers() {
        for (String channelId : channelIds) {
            List<String> memberIds = getChannelMembers(channelId);
            for (String memberId : memberIds) {

                if (memberId.equals(botUserId)) {
                    continue;
                }

                userResponses.put(memberId, new UserResponseTracker(channelId));
                sendQuestion(memberId);
            }
        }
    }

    private List<String> getChannelMembers(String channelId) {
        try {
            ConversationsMembersResponse response = slack.methods(slackToken).conversationsMembers(r -> r.channel(channelId));
            if (response.isOk()) {
                return response.getMembers();
            } else {
                System.err.println("Error fetching channel members: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    private void sendQuestion(String memberId) {
        UserResponseTracker tracker = userResponses.get(memberId);

        if (tracker != null && tracker.isCompleted()) {
            int questionIndex = tracker.getQuestionIndex();

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

        if (tracker == null || responseText.isEmpty()) return;

        tracker.recordResponse(responseText);

        if (tracker.isCompleted()) {
            sendQuestion(userId);
        } else {
            sendSummaryToChannels(userId, tracker);
            userResponses.remove(userId);
        }
    }

    private void sendSummaryToChannels(String userId, UserResponseTracker tracker) {
        String userName = getUserName(userId);

        String currentDate = new SimpleDateFormat("EEEE, MMMM dd, yyyy").format(new Date());
        StringBuilder summary = new StringBuilder("Daily Summary for ").append(userName).append(" on ").append(currentDate).append(": \n");
        for (int i = 0; i < QUESTIONS.length; i++) {
            summary.append(QUESTIONS[i]).append(": ").append(tracker.getResponse(i)).append("\n");
        }

        sendSummaryToChannel(tracker.getChannelId(), summary.toString());
        sendSummaryToChannel(summaryChannelId, summary.toString());
    }

    private void sendSummaryToChannel(String channelId, String summary) {
        try {
            slack.methods(slackToken).chatPostMessage(ChatPostMessageRequest.builder()
                    .channel(channelId)
                    .text(summary)
                    .build());
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }

    private String getUserName(String userId) {
        try {
            UsersInfoResponse response = slack.methods(slackToken).usersInfo(r -> r.user(userId));
            if (response.isOk()) {
                User user = response.getUser();
                return user.getRealName();
            } else {
                System.err.println("Error fetching user info: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return "User (" + userId + ")";
    }
}

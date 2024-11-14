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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DailyBot {

    private final Slack slack;
    private final List<String> channelIds;
    private final String slackToken;
    private final String summaryChannelId;
    private final List<String> targetedMembersEmails;
    private final Map<String, UserResponseTracker> userResponses = new HashMap<>();
    private final Map<String, String> userCurrentChannel = new HashMap<>();
    private final Map<String, Boolean> completedUsers = new HashMap<>();
    private String botUserId;
    private final ExecutorService executorService;

    private static final String[] QUESTIONS = {
            "What did you do yesterday?",
            "What are you doing today?",
            "Do you have any blockers?"
    };

    public static String[] getQuestions() {
        return QUESTIONS;
    }

    public DailyBot(List<String> channelIds, String summaryChannelId, String slackToken, List<String> targetedMembersEmails) {
        this.channelIds = channelIds;
        this.summaryChannelId = summaryChannelId;
        this.slackToken = slackToken;
        this.targetedMembersEmails = targetedMembersEmails;
        this.slack = Slack.getInstance();
        this.executorService = Executors.newCachedThreadPool();

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

    public void sendDailyQuestionsToChannelMembersSequentially() {
        for (String channelId : channelIds) {
            List<String> memberIds = getChannelMembers(channelId);
            for (String memberId : memberIds) {
                if (memberId.equals(botUserId) || completedUsers.containsKey(memberId)) {
                    continue;
                }

                String email = getUserEmailById(memberId);
                if (email == null || !targetedMembersEmails.contains(email)) {
                    continue;
                }

                synchronized (userCurrentChannel) {
                    if (!userCurrentChannel.containsKey(memberId)) {
                        userCurrentChannel.put(memberId, channelId);
                        String userChannelKey = memberId + "-" + channelId;
                        userResponses.put(userChannelKey, new UserResponseTracker(channelId));
                        sendQuestion(memberId, channelId);
                    }
                }
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
        } catch (IOException | SlackApiException exception) {
            exception.printStackTrace();
        }
        return List.of();
    }

    private void sendQuestion(String memberId, String channelId) {
        String userChannelKey = memberId + "-" + channelId;
        UserResponseTracker tracker = userResponses.get(userChannelKey);

        if (tracker != null && !tracker.isCompleted() && !tracker.isAwaitingNextChannel()) {
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

        synchronized (userCurrentChannel) {
            String currentChannelId = userCurrentChannel.get(userId);
            if (currentChannelId != null) {
                String userChannelKey = userId + "-" + currentChannelId;
                UserResponseTracker tracker = userResponses.get(userChannelKey);
                if (tracker == null || responseText.isEmpty()) return;

                tracker.recordResponse(responseText);

                if (!tracker.isCompleted()) {
                    sendQuestion(userId, currentChannelId);
                } else {
                    sendSummaryToChannels(userId, tracker);
                    userResponses.remove(userChannelKey);
                    userCurrentChannel.remove(userId);

                    if (isUserSessionComplete(userId)) {
                        completedUsers.put(userId, true);
                    } else {
                        completedUsers.put(userChannelKey, true);
                        notifyNextChannel(userId);
                    }
                }
            }
        }
    }

    private void notifyNextChannel(String userId) {
        for (String channelId : channelIds) {
            String userChannelKey = userId + "-" + channelId;
            if (!completedUsers.containsKey(userChannelKey) && !userCurrentChannel.containsKey(userId)) {
                UserResponseTracker tracker = userResponses.get(userChannelKey);
                if (tracker == null) {
                    tracker = new UserResponseTracker(channelId);
                } else {
                    tracker.resetTrackerForNextChannel();
                }
                userCurrentChannel.put(userId, channelId);
                userResponses.put(userChannelKey, tracker);
                sendQuestion(userId, channelId);
                break;
            }
        }
    }

    private boolean isUserSessionComplete(String userId) {
        return channelIds.stream().allMatch(channelId -> completedUsers.containsKey(userId + "-" + channelId));
    }

    private void sendSummaryToChannels(String userId, UserResponseTracker tracker) {
        String userName = getUserName(userId);
        String currentDate = new SimpleDateFormat("EEEE, MMMM dd, yyyy").format(new Date());
        StringBuilder summary = new StringBuilder("Daily Summary for ").append(userName).append(" on ").append(currentDate).append(": \n");
        for (int i = 0; i < QUESTIONS.length; i++) {
            summary.append(QUESTIONS[i]).append(": ").append(tracker.getResponse(i)).append("\n");
        }

        sendSummaryToChannel(tracker.getChannelId(), summary.toString());
        sendSummaryToChannel(summaryChannelId, "Summary from channel: " + getChannelNameById(tracker.getChannelId()) + "\n" + summary.toString());
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

    private String getChannelNameById(String channelId) {
        try {
            var response = slack.methods(slackToken).conversationsInfo(r -> r.channel(channelId));
            if (response.isOk()) {
                return response.getChannel().getName();
            } else {
                System.err.println("Error fetching channel name: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return "Channel (" + channelId + ")";
    }

    private String getUserEmailById(String userId) {
        try {
            UsersInfoResponse response = slack.methods(slackToken).usersInfo(r -> r.user(userId));
            if (response.isOk()) {
                return response.getUser().getProfile().getEmail();
            } else {
                System.err.println("Error fetching user email: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return null;
    }
}
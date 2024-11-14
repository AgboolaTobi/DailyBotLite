package com.Bot.dailybot_clone;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DailyBotJob implements Job {
    static DailyBot bot;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void initializeBot(List<String> channelIds, String summaryChannelId, String slackToken, List<String> targetedEmails) {
        if (channelIds == null || channelIds.isEmpty() || summaryChannelId == null) {
            throw new IllegalArgumentException("Channel IDs and Summary Channel ID cannot be null or empty");
        }
        bot = new DailyBot(channelIds, summaryChannelId, slackToken, targetedEmails);
    }

    @Override
    public void execute(JobExecutionContext context) {
        if (bot == null) {
            throw new IllegalStateException("DailyBot has not been initialized");
        }

        executorService.submit(() -> bot.sendDailyQuestionsToChannelMembersSequentially());
    }
}
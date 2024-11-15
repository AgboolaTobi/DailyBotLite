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
        System.out.println("Initializing DailyBot with channels: " + channelIds + " and targeted emails: " + targetedEmails);
        if (channelIds == null || summaryChannelId == null || slackToken == null || targetedEmails == null) {
            throw new IllegalArgumentException("Required parameters cannot be null");
        }
        bot = new DailyBot(slackToken, summaryChannelId, channelIds, targetedEmails);
    }

    @Override
    public void execute(JobExecutionContext context) {
        if (bot == null) {
            throw new IllegalStateException("DailyBot has not been initialized");
        }
        executorService.submit(() -> bot.sendDailyQuestionsToChannelMembersSequentially());
    }
}
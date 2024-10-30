package com.Bot.dailybot_clone;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.List;

public class DailyBotJob implements Job {
    static DailyBot bot;

    public static void initializeBot(List<String> channelIds, String summaryChannelId, String slackToken) {
        if (channelIds == null || channelIds.isEmpty() || summaryChannelId == null) {
            throw new IllegalArgumentException("Channel IDs and Summary Channel ID cannot be null or empty");
        }
        bot = new DailyBot(channelIds, summaryChannelId, slackToken);
    }

    @Override
    public void execute(JobExecutionContext context) {
        if (bot == null) {
            throw new IllegalStateException("DailyBot has not been initialized");
        }
        bot.sendDailyQuestionsToChannelMembers();
    }
}

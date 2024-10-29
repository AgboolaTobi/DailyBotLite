package com.Bot.dailybot_clone;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class DailyBotJob implements Job {
    static DailyBot bot;

    public static void initializeBot(String channelId, String token) {
        bot = new DailyBot(channelId, token);
    }

    @Override
    public void execute(JobExecutionContext context) {
        bot.sendDailyQuestions();
    }
}

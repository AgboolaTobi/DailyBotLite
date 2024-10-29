package com.Bot.dailybot_clone;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class SchedulerSetup {

    public static void startDailyJob(String channelId, String token) {
        DailyBotJob.initializeBot(channelId, token);

        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail job = JobBuilder.newJob(DailyBotJob.class)
                    .withIdentity("dailyBotJob")
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("dailyBotTrigger")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 * * ? * *"))  // Every minute
                    .build();


            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}

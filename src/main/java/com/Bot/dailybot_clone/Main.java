package com.Bot.dailybot_clone;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Main {
	public static void main(String[] args) {
		Properties properties = new Properties();
		try (InputStream input = Main.class.getClassLoader().getResourceAsStream("secret.properties")) {
			if (input == null) {
				System.err.println("File not found.");
				return;
			}
			properties.load(input);
		} catch (IOException ex) {
			System.err.println("Error loading File.");
			ex.printStackTrace();
			return;
		}

		String slackToken = properties.getProperty("slackToken");
		String xappToken = properties.getProperty("socketToken");
		String channelIds = properties.getProperty("slack.channelIds");
		String summaryChannelId = properties.getProperty("slack.summaryChannelId");

		if (slackToken == null || slackToken.isEmpty() || channelIds == null || channelIds.isEmpty() || summaryChannelId == null || summaryChannelId.isEmpty() || xappToken == null || xappToken.isEmpty()) {
			System.err.println("Error: Missing required targeted file.");
			return;
		}

		List<String> channelIdsList = Arrays.asList(channelIds.split(","));


		DailyBotJob.initializeBot(channelIdsList, summaryChannelId, slackToken);

		try {
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();

			JobDetail job = JobBuilder.newJob(DailyBotJob.class)
					.withIdentity("dailyBotJob")
					.build();

			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity("dailyBotTrigger")
					.startNow()
					.withSchedule(CronScheduleBuilder.cronSchedule("0 10 10 ? * MON-FRI"))
					.build();

			scheduler.scheduleJob(job, trigger);


			App app = new App();
			app.event(com.slack.api.model.event.MessageEvent.class, (payload, ctx) -> {
				DailyBotJob.bot.handleResponse(payload.getEvent());
				return ctx.ack();
			});


			SocketModeApp socketModeApp = new SocketModeApp(xappToken, app);
			socketModeApp.start();
		}
		catch (SchedulerException | IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}

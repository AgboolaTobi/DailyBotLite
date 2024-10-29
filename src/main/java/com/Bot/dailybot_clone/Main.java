package com.Bot.dailybot_clone;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
	public static void main(String[] args) throws Exception {

		Properties properties = new Properties();
		try (InputStream input = new FileInputStream("C:/Users/Dell/Documents/bot/dailybot-clone/src/main/resources/secret.properties")) {
			properties.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		}


		String xappToken = properties.getProperty("botToken");
		String xoxbToken = properties.getProperty("slackToken");
		String channelId = properties.getProperty("slack.channelId");



		DailyBotJob.initializeBot(channelId, xoxbToken);


		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();

		JobDetail job = JobBuilder.newJob(DailyBotJob.class)
				.withIdentity("dailyBotJob")
				.build();

		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity("dailyBotTrigger")
				.startNow()
				.withSchedule(CronScheduleBuilder.cronSchedule("0/10 * * * * ?"))
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


}

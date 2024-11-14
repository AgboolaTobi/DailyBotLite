package com.Bot.dailybot_clone;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.response.auth.AuthTestResponse;
import com.slack.api.Slack;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static com.mongodb.client.model.Filters.eq;

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
		String summaryChannelId = properties.getProperty("slack.summaryChannelId");
		String mongoConnectionString = properties.getProperty("mongoConnectionString");

		if (slackToken == null || slackToken.isEmpty() || summaryChannelId == null || summaryChannelId.isEmpty()
				|| xappToken == null || xappToken.isEmpty() || mongoConnectionString == null || mongoConnectionString.isEmpty()) {
			System.err.println("Error: Missing required configuration");
			return;
		}


		try {
			Slack slack = Slack.getInstance();
			AuthTestResponse response = slack.methods(slackToken).authTest(req -> req);
			if (!response.isOk()) {
				System.err.println("Invalid Slack Token: " + response.getError());
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new com.mongodb.ConnectionString(mongoConnectionString))
				.build();

		try (MongoClient mongoClient = MongoClients.create(settings)) {
			MongoDatabase database = mongoClient.getDatabase("DailyBotDB");
			MongoCollection<Document> collection = database.getCollection("TargetedInfo");


			Bson filter = eq("_id", "targeted_info");
			Document targetedInfo = collection.find(filter).first();

			if (targetedInfo == null) {
				System.err.println("Error: No targeted information found in the database.");
				return;
			}


			List<String> channelIdsList = targetedInfo.getList("targetedChannels", String.class);
			List<String> targetedEmailsList = targetedInfo.getList("targetedMembersEmails", String.class);

			if (channelIdsList == null || channelIdsList.isEmpty() || targetedEmailsList == null || targetedEmailsList.isEmpty()) {
				System.err.println("Error: Channel IDs or Targeted Emails are missing in the database document.");
				return;
			}

//            debugging...
			System.out.println("Starting DailyBotJob with channels: " + channelIdsList + " and emails: " + targetedEmailsList);

			DailyBotJob.initializeBot(channelIdsList, summaryChannelId, slackToken, targetedEmailsList);

			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();

			JobDetail job = JobBuilder.newJob(DailyBotJob.class)
					.withIdentity("dailyBotJob")
					.build();


			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity("dailyBotTrigger")
					.startNow()
					.withSchedule(CronScheduleBuilder.cronSchedule("0 16 15 ? * MON-FRI"))
					.build();

			scheduler.scheduleJob(job, trigger);

			App app = new App();
			app.event(com.slack.api.model.event.MessageEvent.class, (payload, ctx) -> {
				DailyBotJob.bot.handleResponse(payload.getEvent());
				return ctx.ack();
			});

			SocketModeApp socketModeApp = new SocketModeApp(xappToken, app);
			socketModeApp.start();
		} catch (SchedulerException e) {
			System.err.println("SchedulerException occurred: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException occurred while starting SocketModeApp: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Unexpected error occurred: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
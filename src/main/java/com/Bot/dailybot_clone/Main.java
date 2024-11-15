package com.Bot.dailybot_clone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slack.api.Slack;
import com.slack.api.methods.response.auth.AuthTestResponse;
import io.github.cdimascio.dotenv.Dotenv;
import spark.Service;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();  // Loading environment variables from .env file

		String slackToken = dotenv.get("SLACK_TOKEN");
		String summaryChannelId = dotenv.get("SUMMARY_CHANNEL_ID");
		String portEnv = dotenv.get("PORT", "8080");
		int port = Integer.parseInt(portEnv);

		// Hard-coded channel IDs and targeted emails(only for testing purposes)
		List<String> channelIds = List.of("C07VD9KLH5W");
		List<String> targetedEmails = List.of("agboola.tobi@nomba.com", "akin.akinbobola@nomba.com");

		// Ensure environment variables are loaded(debugging...)
		if (slackToken == null || slackToken.isEmpty() || summaryChannelId == null || summaryChannelId.isEmpty()) {
			System.err.println("Error: Missing required configuration");
			System.exit(1);
		}

		// Slack Authentication
		try {
			Slack slack = Slack.getInstance();
			AuthTestResponse response = slack.methods(slackToken).authTest(req -> req);
			if (!response.isOk()) {
				System.err.println("Invalid Slack Token: " + response.getError());
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Initializing the bot with Slack token and other required info
		DailyBotJob.initializeBot(channelIds, summaryChannelId, slackToken, targetedEmails);

		// Setting up an HTTP endpoint using SparkJava
		Service http = Service.ignite().port(port);
		System.out.println("Starting server on port: " + port);

		http.post("/event-handler", (req, res) -> {
			JsonObject json = JsonParser.parseString(req.body()).getAsJsonObject();

			// Slack URL Verification Challenge
			if (json.has("challenge")) {
				String challenge = json.get("challenge").getAsString();
				res.status(200);
				res.type("text/plain");  // Ensure the response is plain text
				return challenge;
			}

			// Handle Slack Events
			if (json.has("event")) {
				JsonObject event = json.getAsJsonObject("event");
				String eventType = event.get("type").getAsString();
				if ("message".equals(eventType)) {
					String userId = event.get("user").getAsString();
					String text = event.get("text").getAsString();
					System.out.println("Received a message from user: " + userId + " with text: " + text);
					DailyBotJob.bot.handleResponse(event);
				}
			}

			res.status(200);
			return "ok";
		});

		http.get("/trigger", (req, res) -> {
			// Endpoint to trigger sending daily questions
			DailyBotJob.bot.sendDailyQuestionsNow();
			System.out.println("/trigger endpoint called - Questions sent");
			res.status(200);
			return "Trigger executed";
		});
	}
}

# Stage 1: Building the application
FROM maven:3.8.6-openjdk-11 AS build

# Setting the working directory inside the container
WORKDIR /app

# Copying the POM file and the source code into the container
COPY pom.xml .
COPY src ./src

# Running Maven to build the application and package it into a JAR file
RUN mvn clean package

# Stage 2: Creating the actual Docker image to run the application
FROM openjdk:11-jre-slim

WORKDIR /app

# Copying the JSON file into the container
#COPY bot_data.json /app/bot_data.json

EXPOSE 8080

# Copying the JAR file from the build stage
COPY --from=build /app/target/dailybot_clone-1.0-SNAPSHOT-jar-with-dependencies.jar /app/DailyBotLite.jar

# Running the application
CMD ["java", "-jar", "/app/DailyBotLite.jar"]
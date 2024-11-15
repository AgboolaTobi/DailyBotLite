# Stage 1: Building the application
FROM maven:3.8.6-openjdk-11 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY .env .

RUN mvn clean package -DskipTests

# Stage 2: Creating the actual Docker image to run the application
FROM openjdk:11-jre-slim

WORKDIR /app

EXPOSE 8080

COPY --from=build /app/target/dailybot_clone-1.0-SNAPSHOT.jar /app/DailyBotLite.jar
COPY .env .

CMD ["java", "-jar", "/app/DailyBotLite.jar"]

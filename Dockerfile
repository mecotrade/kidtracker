FROM openjdk:8
MAINTAINER mecotrade

RUN mkdir -p /app
ADD kidtracker*.jar /app/kidtracker.jar

RUN mkdir -p /app/data

WORKDIR /app
CMD java -jar kidtracker.jar
FROM openjdk:8
MAINTAINER Sergey Shadchin (sergei.shadchin@gmail.com)

RUN mkdir -p /app
ADD kidtracker*.jar /app/kidtracker.jar

RUN mkdir -p /app/data

WORKDIR /app
CMD java -jar kidtracker.jar
FROM maven:3-jdk-8
MAINTAINER Sergey Shadchin (sergei.shadchin@gmail.com)

RUN \
  apt-get update && \
  apt-get -y install nodejs npm

RUN npm install npm@latest -g

RUN mkdir -p /build
ADD build.sh /usr/local/bin/build.sh

RUN chmod u+x /usr/local/bin/build.sh

WORKDIR /build
CMD build.sh
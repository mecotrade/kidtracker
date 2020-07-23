FROM maven:3.6.3-jdk-8
MAINTAINER mecotrade

RUN \
  apt-get update && \
  apt-get -y install nodejs npm

RUN mkdir -p /build
ADD build.sh /usr/local/bin/build-entrypoint.sh

WORKDIR /build
CMD build-entrypoint.sh
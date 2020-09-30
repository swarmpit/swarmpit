FROM debian:buster-slim

RUN apt-get update && \
    mkdir -p /usr/share/man/man1 && \
    apt-get install -y ca-certificates curl openjdk-11-jre-headless libjffi-java

ADD dev/script/install-docker-client.sh .
RUN bash install-docker-client.sh

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY target/swarmpit.jar /usr/src/app/

RUN mkdir -p /usr/logs/app
ENV JAVA_OPTS "-XX:LogVMOutput \
               -XX:LogFile=/usr/logs/app/jvm.log"

EXPOSE 8080
CMD java $JAVA_OPTS -jar swarmpit.jar
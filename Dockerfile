FROM debian:stable-slim

RUN apt-get update && \
    mkdir -p /usr/share/man/man1 && \
    apt-get install -y ca-certificates curl openjdk-17-jre-headless libjffi-java

ADD dev/script/install-docker-client.sh .
RUN bash install-docker-client.sh

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY target/swarmpit.jar /usr/src/app/

HEALTHCHECK CMD curl --fail -s http://localhost:8080

EXPOSE 8080
CMD ["java", "-jar", "swarmpit.jar"]

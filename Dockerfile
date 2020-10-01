FROM debian:buster-slim

RUN apt-get update && \
    mkdir -p /usr/share/man/man1 && \
    apt-get install -y ca-certificates curl unzip openjdk-11-jre-headless libjffi-java && \
    curl https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2020.9-docker.zip -k --output /tmp/YourKit-JavaProfiler-2020.9-docker.zip && \
    unzip /tmp/YourKit-JavaProfiler-2020.9-docker.zip -d /usr/local && \
    rm /tmp/YourKit-JavaProfiler-2020.9-docker.zip

ADD dev/script/install-docker-client.sh .
RUN bash install-docker-client.sh

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY target/swarmpit.jar /usr/src/app/

EXPOSE 8080 10001
CMD java $JAVA_OPTS -jar swarmpit.jar
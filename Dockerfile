FROM openjdk:8u212-alpine
MAINTAINER Pavol Noha <pavol.noha@gmail.com>

RUN apk add --update curl docker && \
    rm -rf /var/cache/apk/* && \
    rm /usr/bin/dockerd /usr/bin/containerd /usr/bin/runc

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY target/swarmpit.jar /usr/src/app/

EXPOSE 8080
CMD ["java", "-jar", "swarmpit.jar"]

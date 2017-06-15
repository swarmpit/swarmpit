FROM java:8-alpine
MAINTAINER Pavol Noha <pavol.noha@gmail.com>

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY target/swarmpit.jar /usr/src/app/

EXPOSE 8080
CMD ["java", "-jar", "swarmpit.jar"]
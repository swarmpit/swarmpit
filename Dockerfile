FROM java:8-alpine
MAINTAINER Pavol Noha <pavol.noha@gmail.com>

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*

ENV VERSION "18.03.0-ce"
RUN curl -L -o /tmp/docker-$VERSION.tgz https://download.docker.com/linux/static/stable/x86_64/docker-$VERSION.tgz \
    && tar -xz -C /tmp -f /tmp/docker-$VERSION.tgz \
    && mv /tmp/docker/docker /usr/bin \
    && rm -rf /tmp/docker-$VERSION /tmp/docker

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY target/swarmpit.jar /usr/src/app/

EXPOSE 8080
CMD ["java", "-jar", "swarmpit.jar"]
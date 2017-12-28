#!/bin/bash

if [ "$(docker ps -aq -f name=event-collector)" ];
then
   	echo "Event collector already exists."
    if [ "$(docker ps -aq -f status=exited -f name=event-collector)" ];
    then
        echo "Event collector down. Starting ..."
        docker start event-collector
        sleep 5
    else
        echo "Event collector running."
    fi
else
    echo "Creating event collector"
    docker run -d \
      --name event-collector \
      --env EVENT_ENDPOINT=http://192.168.65.1:3449/events \
      --env HEALTH_CHECK_ENDPOINT=http://192.168.65.1:3449/version \
      --volume /var/run/docker.sock:/var/run/docker.sock \
      swarmpit/event-collector:latest
fi
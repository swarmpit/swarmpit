#!/bin/bash

if [ "$(docker ps -aq -f name=swarmpitagent)" ];
then
   	echo "Swarmpit agent already exists."
    if [ "$(docker ps -aq -f status=exited -f name=swarmpitagent)" ];
    then
        echo "Swarmpit agent down. Starting ..."
        docker start swarmpitagent
    else
        echo "Swarmpit agent running."
    fi
else
    echo "Creating swarmpit agent"
    docker run -d \
      --publish 8888:8080 \
      --name swarmpitagent \
      --env DOCKER_API_VERSION=1.30 \
      --env EVENT_ENDPOINT=http://192.168.65.2:3449/events \
      --env HEALTH_CHECK_ENDPOINT=http://192.168.65.2:3449/version \
      --volume /var/run/docker.sock:/var/run/docker.sock \
      swarmpit/agent:latest
fi
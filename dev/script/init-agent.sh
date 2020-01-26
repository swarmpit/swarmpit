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
    detected=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')
    host_address=${detected:-"172.17.0.1"}
    docker run -d \
      --publish 8888:8080 \
      --name swarmpitagent \
      --env DOCKER_API_VERSION=1.30 \
      --env EVENT_ENDPOINT=http://${host_address}:3449/events \
      --env HEALTH_CHECK_ENDPOINT=http://${host_address}:3449/version \
      --volume /var/run/docker.sock:/var/run/docker.sock \
      swarmpit/agent:latest
fi
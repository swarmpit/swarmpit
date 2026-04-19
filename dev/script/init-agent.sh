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
    # Detect host IP accessible from containers
    if [ "$(uname)" = "Darwin" ]; then
      HOST_IP="host.docker.internal"
    else
      HOST_IP=$(ip -4 addr show docker0 2>/dev/null | grep -oP 'inet \K[\d.]+' || echo "172.17.0.1")
    fi
    docker run -d \
      --publish 8888:8080 \
      --name swarmpitagent \
      --env DOCKER_API_VERSION=1.44 \
      --env EVENT_ENDPOINT=http://${HOST_IP}:3449/events \
      --env HEALTH_CHECK_ENDPOINT=http://${HOST_IP}:3449/version \
      --volume /var/run/docker.sock:/var/run/docker.sock \
      swarmpit/agent:latest
fi
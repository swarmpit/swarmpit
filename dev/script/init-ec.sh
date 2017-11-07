#!/bin/bash

if [ "$(docker ps -aq -f name=swarmpitec)" ];
then
   	echo "Swarmpit EC already exists."
    if [ "$(docker ps -aq -f status=exited -f name=swarmpitec)" ];
    then
        echo "Swarmpit EC down. Starting ..."
        docker start swarmpitec
        sleep 5
    else
        echo "Swarmpit EC running."
    fi
else
    echo "Creating swarmpit EC"
    docker run -d \
      --name swarmpitec \
      --env EVENT_ENDPOINT=http://192.168.65.1:3449/events \
      --volume /var/run/docker.sock:/var/run/docker.sock \
      swarmpit/swarmpit-ec:latest
fi
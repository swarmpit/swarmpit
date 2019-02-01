#!/bin/bash

if [ "$(docker ps -aq -f name=swarmpitdb)" ];
then
   	echo "Swarmpit DB already exists."
    if [ "$(docker ps -aq -f status=exited -f name=swarmpitdb)" ];
    then
        echo "Swarmpit DB down. Starting ..."
        docker start swarmpitdb
    else
        echo "Swarmpit DB running."
    fi
else
    echo "Creating swarmpit DB"
    docker run -d -p 5984:5984 --name swarmpitdb couchdb:2.3.0
fi
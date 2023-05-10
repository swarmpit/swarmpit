#!/bin/bash

if [ "$(docker ps -aq -f name=swarmpitinflux)" ];
then
   	echo "Swarmpit InfluxDB already exists."
    if [ "$(docker ps -aq -f status=exited -f name=swarmpitinflux)" ];
    then
        echo "Swarmpit InfluxDB down. Starting ..."
        docker start swarmpitinflux
    else
        echo "Swarmpit InfluxDB running."
    fi
else
    echo "Creating swarmpit InfluxDB"
    docker run -d -p 8086:8086 --name swarmpitinflux influxdb:1.7
fi
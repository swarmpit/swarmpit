#!/usr/bin/env bash

docker run --privileged --name dind -p 12375:2375 -d docker:$DOCKER-dind
sleep 3
docker -H tcp://localhost:12375 version
docker -H tcp://localhost:12375 swarm init
docker -H tcp://localhost:12375 load -i $(dirname "${BASH_SOURCE[0]}")/hello-world.tar
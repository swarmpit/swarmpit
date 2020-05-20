#!/usr/bin/env bash

docker run --privileged --name dind -p 12375:2375 -e DOCKER_TLS_CERTDIR='' -d docker:$DOCKER-dind
sleep 3
export DOCKER_API_VERSION=1.30
docker -H tcp://localhost:12375 version
docker -H tcp://localhost:12375 swarm init
docker -H tcp://localhost:12375 load -i $(dirname "${BASH_SOURCE[0]}")/hello-world.tar

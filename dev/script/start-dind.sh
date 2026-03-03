#!/usr/bin/env bash

docker run --privileged --name dind -p 12375:2375 -e DOCKER_TLS_CERTDIR='' -d docker:$DOCKER-dind
export DOCKER_API_VERSION=1.44
echo "Waiting for Docker daemon..."
for i in $(seq 1 30); do
  docker -H tcp://localhost:12375 version &>/dev/null && break
  sleep 1
done
docker -H tcp://localhost:12375 version
docker -H tcp://localhost:12375 swarm init
docker -H tcp://localhost:12375 load -i $(dirname "${BASH_SOURCE[0]}")/hello-world.tar

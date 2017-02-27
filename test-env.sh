#!/bin/bash

vm=swarmpit
docker-machine create --driver virtualbox --virtualbox-memory 1024 $vm
eval $(docker-machine env $vm)
docker swarm init --advertise-addr $(docker-machine ip $vm)

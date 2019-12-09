#!/usr/bin/env bash

echo '{"experimental":true}' | sudo tee /etc/docker/daemon.json
mkdir $HOME/.docker
touch $HOME/.docker/config.json
echo '{"experimental":"enabled"}' | sudo tee $HOME/.docker/config.json
sudo service docker restart
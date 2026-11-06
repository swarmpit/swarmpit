#!/bin/bash
set -eu

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
    : "${SWARMPIT_DB_USERNAME:?Set SWARMPIT_DB_USERNAME for the development CouchDB administrator}"
    : "${SWARMPIT_DB_PASSWORD:?Set SWARMPIT_DB_PASSWORD for the development CouchDB administrator}"
    repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
    docker run -d -p 127.0.0.1:5984:5984 --name swarmpitdb --memory 512m \
      -e COUCHDB_USER="$SWARMPIT_DB_USERNAME" \
      -e COUCHDB_PASSWORD="$SWARMPIT_DB_PASSWORD" -e "ERL_FLAGS=+S 2:2" \
      -v "$repo_dir/couchdb.ini:/opt/couchdb/etc/local.d/10-swarmpit.ini:ro" \
      --user 5984:5984 couchdb:3.5.3
fi
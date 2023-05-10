#!/usr/bin/env bash
set -e
ORG=swarmpit
REPO=swarmpit
NAMESPACE=$ORG/$REPO
TAG=${TRAVIS_BRANCH/\//-}

if [ $DOCKER != "stable" ]
then
	echo "images are build and deployed only for stable docker build"
	exit 0
fi

if [ $TRAVIS_PULL_REQUEST == "false" ]
then
    if [ $TAG == "master" ]
    then
        TAG="latest"
    fi
else
    TAG="pr-$TRAVIS_PULL_REQUEST-$TAG"
fi

IMAGE=$NAMESPACE:$TAG

lein uberjar
docker run --rm --privileged docker/binfmt:820fdd95a9972a5308930a2bdfb8573dd4447ad3
docker buildx create --name multi
docker buildx use multi
docker buildx inspect --bootstrap
docker buildx build --platform linux/amd64,linux/arm64,linux/arm/v7,linux/arm/v6 -t $IMAGE .

if [ $TRAVIS_SECURE_ENV_VARS == "true" ] 
then
	docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
    docker buildx build --platform linux/amd64,linux/arm64,linux/arm/v7,linux/arm/v6 -t $IMAGE --push .
else
	echo "images can be pushed only from base repo"
fi
#!/usr/bin/env bash
ORG=swarmpit
REPO=swarmpit
NAMESPACE=$ORG/$REPO

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker tag $REPO "$NAMESPACE:build-$TRAVIS_BUILD_NUMBER"
docker tag $REPO "$NAMESPACE:sha-$TRAVIS_COMMIT"
if [ $TRAVIS_PULL_REQUEST == "false" ]
then
		docker tag $REPO "$NAMESPACE:$TRAVIS_BRANCH"

		if [ $TRAVIS_BRANCH == "master" ]
		then
			docker tag $REPO "$NAMESPACE:latest"
		fi
fi
docker push "$NAMESPACE"

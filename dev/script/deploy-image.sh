#!/usr/bin/env bash
ORG=swarmpit
REPO=swarmpit
NAMESPACE=$ORG/$REPO

if [ $TRAVIS_PULL_REQUEST == "false" ]
then
	docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
	docker tag $REPO "$NAMESPACE:build-$TRAVIS_BUILD_NUMBER"
	docker tag $REPO "$NAMESPACE:sha-$TRAVIS_COMMIT"
	docker tag $REPO "$NAMESPACE:$TRAVIS_BRANCH"

		if [ $TRAVIS_BRANCH == "master" ]
		then
			docker tag $REPO "$NAMESPACE:latest"
		fi
	
	docker push "$NAMESPACE"
else
	echo "pull-requests are not deployed"
fi

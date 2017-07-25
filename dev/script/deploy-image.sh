#!/usr/bin/env bash
ORG=swarmpit
REPO=swarmpit
NAMESPACE=$ORG/$REPO
BRANCH=${TRAVIS_BRANCH/\//-}

if [ $DOCKER != "stable" ]
then
	echo "images are deployed only for stable docker build"
	exit 0
fi

if [ $CONTRIBUTOR == "true" ] 
then
	docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
	
	if [ $TRAVIS_PULL_REQUEST == "false" ]
	then
		docker tag $REPO "$NAMESPACE:$BRANCH"

			if [ $BRANCH == "master" ]
			then
				docker tag $REPO "$NAMESPACE:latest"
			fi
	else
		docker tag $REPO "$NAMESPACE:pr-$TRAVIS_PULL_REQUEST"
	fi
	
	docker push "$NAMESPACE"
else
	echo "images can be pushed only from base repo"
fi

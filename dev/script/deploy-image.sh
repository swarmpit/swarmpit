#!/usr/bin/env bash
ORG=swarmpit
REPO=swarmpit
NAMESPACE=$ORG/$REPO
BRANCH=${TRAVIS_BRANCH/\//-}

if [ $DOCKER != "stable" ]
then
	echo "images are build and deployed only for stable docker build"
	exit 0
fi

lein uberjar || exit 1
docker build -t $REPO . || exit 1

if [ $CONTRIBUTOR == "true" ] 
then
	docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
	
	if [ $TRAVIS_PULL_REQUEST == "false" ]
	then
		echo    "tagging  $NAMESPACE:$BRANCH"
		docker tag $REPO "$NAMESPACE:$BRANCH"

			if [ $BRANCH == "master" ]
			then
				echo    "tagging  $NAMESPACE:latest"
				docker tag $REPO "$NAMESPACE:latest"
			fi
	else
		echo    "tagging  $NAMESPACE:pr-$TRAVIS_PULL_REQUEST"
		docker tag $REPO "$NAMESPACE:pr-$TRAVIS_PULL_REQUEST"
	fi
	
	docker push "$NAMESPACE" || exit 1
else
	echo "images can be pushed only from base repo"
fi

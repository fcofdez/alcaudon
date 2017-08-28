#!/bin/bash

set -ev

# Deploy if TRAVIS_TAG is set.
# Error if TRAVIS_SECURE_ENV_VARS is false

[ -z "${TRAVIS_TAG}" ] && exit 0;
[ ! ${TRAVIS_SECURE_ENV_VARS} ] && exit -1;

. $(dirname $0)/travis-decode.sh

docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"

sbt -jvm-opts travis/jvmopts.compile -Dproject.version=$TRAVIS_TAG publishSigned sonatypeRelease docker:publish

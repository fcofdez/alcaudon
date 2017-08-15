#!/bin/bash

set -ev

[ ! ${TRAVIS_SECURE_ENV_VARS} ] && exit -1;

openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in secring.gpg.enc -out secring.gpg -d
openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in pubring.gpg.enc -out pubring.gpg -d

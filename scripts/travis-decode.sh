#!/bin/bash

set -ev

[ ! ${TRAVIS_SECURE_ENV_VARS} ] && exit -1;

openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -md sha256 -in secring.gpg.enc -out secring.gpg -d
openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -md sha256 -in pubring.gpg.enc -out pubring.gpg -d

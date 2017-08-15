#!/bin/bash

set -ev

sbt ++$TRAVIS_SCALA_VERSION test

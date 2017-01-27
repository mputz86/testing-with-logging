#!/bin/bash

pushd ./elk
./build.sh
popd

pushd ./redis
./build.sh
popd

pushd ./user-service
sbt docker
popd

pushd ./shop-service
sbt docker
popd

pushd ./test-server
sbt docker
popd


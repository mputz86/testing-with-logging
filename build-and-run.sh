#!/bin/bash

pushd docker-example
./build.sh
popd

sbt compile
sbt test

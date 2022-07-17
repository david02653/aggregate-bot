#!/bin/bash

mvn clean install -Dmaven.test.skip=true
cp ./target/msdobot-0.0.1-SNAPSHOT.jar app.jar

docker build . -t msdobot/msdobot:v1
#!/usr/bin/env bash

ant

cd ./bin
# start the rmiregistry
"$JAVA_HOME/bin/rmiregistry" &

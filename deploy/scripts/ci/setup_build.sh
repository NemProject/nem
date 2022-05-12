#!/bin/bash

set -ex

# build core 
pushd ../core
./setup_java9.sh
 mvn install -DskipTests=true -B
popd

./setup_java9.sh


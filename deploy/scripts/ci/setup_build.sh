#!/bin/bash

set -ex

# build core
pushd ../core
 mvn install -DskipTests=true -B
popd

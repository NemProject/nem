#!/bin/bash

set -ex

# build deps
for folder in core deploy peer
do
pushd "../${folder}"
 mvn install -DskipTests=true -B
popd
done

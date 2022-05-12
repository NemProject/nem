#!/bin/bash

set -ex

# build deps
for folder in core deploy peer
do
pushd "../${folder}"
./setup_java9.sh
 mvn install -DskipTests=true -B
popd
done

./setup_java9.sh


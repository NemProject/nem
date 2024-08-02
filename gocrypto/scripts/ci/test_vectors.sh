#!/bin/bash

set -ex

TEST_RUNNER="go test -v -run TestVectors"
if [ "$1" = "code-coverage" ]; then
  TEST_RUNNER+=" -race -cover -covermode=atomic -args -test.gocoverdir=.coverage"
fi

${TEST_RUNNER}

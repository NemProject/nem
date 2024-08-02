#!/bin/bash

set -ex

TEST_RUNNER="go test -v -skip TestVectors"
if [ "$1" = "code-coverage" ]; then
  TEST_RUNNER+=" -race -cover -covermode=atomic -args -test.gocoverdir=.coverage"
  export CGO_ENABLED=1
  mkdir -p .coverage
fi

${TEST_RUNNER}

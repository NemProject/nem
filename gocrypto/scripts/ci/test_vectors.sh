#!/bin/bash

set -ex

TEST_RUNNER="go test -v -run TestVectors"
if [ "$1" = "code-coverage" ]; then
  TEST_RUNNER+=" -race -cover -covermode=atomic -args -test.gocoverdir=.coverage"
  export CGO_ENABLED=1
fi

${TEST_RUNNER}

# merge the coverage files
if [ "$1" = "code-coverage" ]; then
  go tool covdata textfmt -i=.coverage -o=coverage.out
  rm -rf .coverage
fi
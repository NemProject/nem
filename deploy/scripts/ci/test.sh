#!/bin/bash

set -ex

mvn test -B
mvn failsafe:integration-test -B


#!/bin/bash

set -ex

npm run lint
../_symbol/sdk/java/gradlew --no-daemon spotlessCheck \
	checkJavaSnippetLineLength
bash scripts/ci/lint_python.sh

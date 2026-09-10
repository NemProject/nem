#!/bin/bash

set -ex

npm run lint
bash scripts/ci/lint_python.sh

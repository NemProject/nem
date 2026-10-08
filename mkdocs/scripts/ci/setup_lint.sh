#!/bin/bash

set -ex

npm install

python3 -m pip install -r "$(git rev-parse --show-toplevel)/linters/python/lint_requirements.txt"
python3 -m pip install -r requirements.txt

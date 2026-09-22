#!/bin/bash

set -ex

# Silence warning from Material about incompatible upcoming MkDocs version
export NO_MKDOCS_2_WARNING=1
# Silence warning from some other plugins about incompatible upcoming MkDocs version
export DISABLE_MKDOCS_2_WARNING=true

# Run from the mkdocs folder
mkdocs build -f config/mkdocs.en.yml --verbose
mkdocs build -f config/mkdocs.ja.yml --verbose

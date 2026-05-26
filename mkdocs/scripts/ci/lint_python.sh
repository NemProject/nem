#!/bin/bash

set -ex

find . \( -name node_modules -o -name .venv \) -prune -o -type f -name "*.sh" -print0 | xargs -0 shellcheck
find snippets \( -name node_modules -o -name .venv \) -prune -o -type f -name "*.py" -print0 | PYTHONPATH=. xargs -0 python3 -m isort \
	--line-length 75 \
	--indent "	" \
	--multi-line 3 \
	--check-only
find snippets \( -name node_modules -o -name .venv \) -prune -o -type f -name "*.py" -print0 | PYTHONPATH=. xargs -0 python3 -m pycodestyle \
	--config=.pycodestyle

# Build a custom .pylintrc file based on the global one
TMP_RC_FILE=/tmp/symbol-docs.pylintrc
cp "$(git rev-parse --show-toplevel)/linters/python/.pylintrc" "$TMP_RC_FILE"
{
	# Allow lowercase "constants" (actually, top-level regular variables)
	echo "const-rgx=(([A-Za-z_][A-Za-z0-9_]*)|(t_[A-Z0-9_]+)|(__.*__))$"
	# Disable some warnings we accept for tutorial code
	echo "disable=missing-docstring,broad-exception-caught,duplicate-code,use-maxsplit-arg,too-many-locals,too-many-branches,too-many-statements"
	# Do not check these modules, as we do not install them to build the docs
	echo "ignored-modules=web3,websockets"
} >> $TMP_RC_FILE
find snippets \( -name node_modules -o -name .venv \) -prune -o -type f -name "*.py" -print0 | PYTHONPATH=. xargs -0 python3 -m pylint \
	--rcfile $TMP_RC_FILE

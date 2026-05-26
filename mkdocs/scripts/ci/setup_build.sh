#!/bin/bash

set -ex

# Install Typedoc
npm install

# Build Typescript SDK
pushd ../sdk/javascript
npm install
npx tsc -p ./tsconfig/build-bindings.json
popd

# Build OpenAPI spec
pushd ../openapi
npm install
npm run build:openapi-yaml
popd

# Patch the ezglossary plugin, ignoring errors if it was already patched
ez_root="$(pip show mkdocs-ezglossary-plugin | sed -n 's/Location: \(.*\)/\1/p')"
ez_plugin="${ez_root}/mkdocs_ezglossary_plugin/plugin.py"
patch "${ez_plugin}" scripts/ci/ezglossary.patch --force --ignore-whitespace --fuzz 0 || true

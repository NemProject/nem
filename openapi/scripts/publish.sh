#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

VERSION="$(npm run version --silent)"
TAG="${OPENAPI_RELEASE_TAG:-openapi/v${VERSION}}"
OUTPUT_DIR="_build/v${VERSION}"

echo "Preparing OpenAPI release artifacts for ${TAG}"
npm install
npm run build
npm run version:prepare
npm run version:pack

echo "Artifacts prepared:"
echo " - _build/openapi3.yml"
echo " - _build/openapi3.json"
echo " - _build/postman.json"
echo " - ${OUTPUT_DIR}/openapi3.yml"
echo " - ${OUTPUT_DIR}/openapi3.json"
echo " - ${OUTPUT_DIR}/postman.json"

if [[ -n "${OPENAPI_RELEASE_CREATE_GH:-}" ]]; then
	if ! command -v gh >/dev/null 2>&1; then
		echo "Error: gh CLI is required when OPENAPI_RELEASE_CREATE_GH is set." >&2
		exit 1
	fi

	echo "Creating GitHub release ${TAG}"
	gh release create "${TAG}" \
		"_build/openapi3.yml" \
		"_build/openapi3.json" \
		"_build/postman.json" \
		--title "OpenAPI ${VERSION}" \
		--notes "Automated OpenAPI artifact release."
fi

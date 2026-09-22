#!/bin/bash

set -ex

export NO_MKDOCS_2_WARNING=1
export DISABLE_MKDOCS_2_WARNING=true
export NEM_DOCS_DISABLE_TS=false
export NEM_DOCS_DISABLE_JAVA=false

mkdocs build -f config/mkdocs.en.yml
mkdocs build -f config/mkdocs.ja.yml

cd ../docs
mv en en2
mv ja ja2
git checkout gh-pages
rm -rf en ja
mv en2 en
mv ja2 ja
git add -f en ja
git commit -m "[docs] Update"
git push
git checkout new-docs
cd ../mkdocs

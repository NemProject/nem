#!/bin/bash

set -ex

git commit -m "[docs]: release new docs"
git remote set-url origin https://github.com/NemProject/nem
git remote -v
git push -f origin main:gh-pages

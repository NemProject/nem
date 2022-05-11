#!/bin/bash

ver=$(cat version.current.txt)

echo " [+] RESETING REPOSITORIES and SETTING MASTER"
git submodule foreach git reset --hard
git reset --hard

git submodule foreach git checkout main
git submodule foreach git pull

git submodule foreach git merge --no-ff --no-commit dev
git submodule foreach git commit --author="nembuildbot <nembuildbot@127.0.0.1>"
git submodule foreach git push origin
git submodule foreach git tag --force "v${ver}"
git submodule foreach git push --tags
git submodule foreach git checkout dev

# reset dev branch on top of main
git submodule foreach git checkout -B dev origin/main
git submodule foreach git push origin dev


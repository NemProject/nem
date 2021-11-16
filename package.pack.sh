#!/bin/bash

packageName="nis-`ls package/nis/nem-infra* | sed 's/.*-\(.*\)-BETA.*/\1/'`"

echo " [+] CREATING zip"
zip -r -9 $packageName.zip package > /dev/null
sha256sum $packageName.zip

echo " [+] CREATING tgz"
tar -czpf $packageName.tgz package
sha256sum $packageName.tgz

#!/bin/bash

packageName="nis-$(ls package/nis/nem-infra* | sed 's/.*-\(.*\)\jar*/\1/')"

echo " [+] CREATING tgz"
tar -czpf $packageName.tgz package
sha256sum $packageName.tgz

#!/bin/bash

set -ex

echo $(mvn -f $(git rev-parse --show-toplevel) -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q) > version.txt

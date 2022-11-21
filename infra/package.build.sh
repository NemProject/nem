#!/bin/bash
set -e

logfile=$1

pushd "$(git rev-parse --show-toplevel)"
echo " [+] STARTING BUILD (this might take some time) ${logfile}"
rm -rf core/src/main/resources/nemesis-mijin*
rm -rf core/target/classes/nemesis-mijin*
rm -rf nis/src/main/resources/*mijin*
rm -rf nis/target/classes/*mijin*

mvn clean install -DskipTests=true | tee -a $logfile
pushd core
echo " [+] CREATING DOCS"
mvn javadoc:javadoc >>$logfile
popd
popd

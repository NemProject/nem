#!/bin/bash
set -e

logfile=$1

echo " [+] STARTING BUILD (this might take some time) ${logfile}"
rm -rf nem.core/src/main/resources/nemesis-mijin*
rm -rf nem.core/target/classes/nemesis-mijin*
rm -rf nis/src/main/resources/*mijin*
rm -rf nis/target/classes/*mijin*

mvn clean package install -DskipTests=true | tee -a $logfile
cd nem.core
echo " [+] CREATING DOCS"
mvn javadoc:javadoc >>$logfile
cd ..

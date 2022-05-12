#!/bin/bash

if [ $# -gt 0 ]; then
    mode=mainnet
fi

echo -n " [+] CREATING >>"
echo -n $mode | tr '[:lower:]' '[:upper:]'
echo "<< PACKAGE"

rm -rf package/libs/*.jar package/nis/*.jar

cp -r ../nis/target/libs/*.jar package/libs
rm package/libs/nem-*
cp -r ../nis/target/libs/nem-*.jar package/nis
cp -r ../nis/target/nem-*.jar package/nis

cd package/nis
sed -i "s/nem.network = .*/nem.network = $mode\r/" config.properties
cd ../..

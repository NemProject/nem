#!/bin/bash

cd nis
java -Xms4G -Xmx6G -cp ".:./*:../libs/*" org.nem.deploy.CommonStarter
cd -

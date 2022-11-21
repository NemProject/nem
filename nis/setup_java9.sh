#!/bin/bash

if [[ "$(java -version 2>&1 | sed -Ee 's/.*"(.*)"$/\1/g;s/^([0-9]+\.[0-9]+\.)([0-9]+)[^0-9]/\1\2/g;q')" = "1.8."* ]]; then
	echo "detected java 8, pom is good"
else
	echo "detected java 9+, modifying pom"
	case "$OSTYPE" in
		darwin*)  PLATFORM="OSX" ;;
		linux*)   PLATFORM="LINUX" ;;
		bsd*)     PLATFORM="BSD" ;;
		*)        PLATFORM="UNKNOWN" ;;
	esac

	if [[ "$PLATFORM" == "OSX" || "$PLATFORM" == "BSD" ]]; then
			sed -i ""  "s/<source>1.8<\/source>/<release>8<\/release>/g" pom.xml
			sed -i "" "/<target>1.8<\/target>/d" pom.xml
	elif [ "$PLATFORM" == "LINUX" ]; then
			sed -i "s/<source>1.8<\/source>/<release>8<\/release>/g" pom.xml
			sed -i "/<target>1.8<\/target>/d" pom.xml
	fi
fi

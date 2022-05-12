rem batch files have funny escaping
rem

awk "{ if (match($0, /(.*\.)([0-9]+)(-BETA.*)/, arr)) { printf \"%%s%%d%%s\n\", arr[1], arr[2]+1, arr[3] } else { print } }" < pom.xml > pom.out
tr --delete "\r" < pom.out > pom.xml
rm pom.out

git add pom.xml
sed -n "/BETA/{ s/.*\([0-9]\+.[0-9]\+.[0-9]\+-BETA\).*/bump version to \1/; p }" pom.xml | xargs -iXX git commit -m XX

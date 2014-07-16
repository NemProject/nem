rem batch files have funny escaping
rem

awk "{ if (match($0, /(.*\.)([0-9]+)(-ALPHA.*)/, arr)) { printf \"%%s%%d%%s\n\", arr[1], arr[2]+1, arr[3] } else { print } }" < pom.xml > pom.out
tr --delete "\r" < pom.out > pom.xml

awk "{ if (match($0, /(.*\.)([0-9]+)(-ALPHA.*)/, arr)) { printf \"%%s%%d%%s\n\", arr[1], arr[2]+1, arr[3] } else { print } }" < pom-core.xml > pom.out
tr --delete "\r" < pom.out > pom-core.xml

awk "{ if (match($0, /(.*\.)([0-9]+)(-ALPHA.*)/, arr)) { printf \"%%s%%d%%s\n\", arr[1], arr[2]+1, arr[3] } else { print } }" < pom-mariadb.xml > pom.out
tr --delete "\r" < pom.out > pom-mariadb.xml
rm pom.out

git add pom.xml
git add pom-core.xml
git add pom-mariadb.xml
sed -n "/ALPHA/{ s/.*\([0-9]\+.[0-9]\+.[0-9]\+-ALPHA\).*/bump version to \1/; p }" pom.xml | xargs -iXX git commit -m XX

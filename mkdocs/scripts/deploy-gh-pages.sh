NEM_DOCS_FULL=true mkdocs build -f config/mkdocs.en.yml
NEM_DOCS_FULL=true mkdocs build -f config/mkdocs.ja.yml
cd ../docs
mv en en2
mv ja ja2
git checkout gh-pages
rm -rf en ja
mv en2 en
mv ja2 ja
git add -f en ja
git commit -m "[docs] Update"
git push
git checkout new-docs
cd ../mkdocs

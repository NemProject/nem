from pathlib import Path

import mkdocs_gen_files

nav = mkdocs_gen_files.Nav()

# Generate index file for the TypeScript API ref
for f in mkdocs_gen_files.editor.FilesEditor.current().files:
	if not f.src_uri.startswith("devbook/reference/ts/"):
		continue
	path = Path(f.src_uri.removeprefix("devbook/reference/ts/"))
	if path.stem == "README" or path.stem == ".meta":
		continue
	if "symbol" in path.parts:
		continue

	module_path = path.relative_to(".").with_suffix("")

	p = [i for i in module_path.parts if i not in ["namespaces", "classes", "functions"]]
	parts = tuple(p)

	nav[parts] = path.as_posix()

with mkdocs_gen_files.open("devbook/reference/ts/links.md", "w") as nav_file:
	# Exclude the index file from being indexed by the search plugin
	nav_file.writelines(["---\n", "search:\n", "    exclude: true\n", "---\n\n"])
	nav_file.writelines(nav.build_literate_nav())

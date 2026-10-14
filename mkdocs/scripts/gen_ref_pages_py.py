from pathlib import Path

import mkdocs_gen_files

nav = mkdocs_gen_files.Nav()
ignored_files = mkdocs_gen_files.config['extra']['nem']['py-sdk']['ignore-files']
ignored_folders = mkdocs_gen_files.config['extra']['nem']['py-sdk']['ignore-folders']

root = Path(__file__).parent.parent.parent
src = root / "_symbol/sdk/python/symbolchain"
paths = sorted(src.rglob("*.py"))

for path in paths:
	module_path = path.relative_to(src.parent).with_suffix("")
	doc_path = path.relative_to(src).with_suffix(".md")
	full_doc_path = Path("devbook/reference/py", doc_path)

	parts = tuple(module_path.parts)

	if parts[-1] == "__init__":
		parts = parts[:-1]
	if parts[-1] in ignored_files:
		continue
	if any(e in parts for e in ignored_folders):
		continue

	nav[parts] = doc_path.as_posix()

	with mkdocs_gen_files.open(full_doc_path, "w") as fd:
		identifier = ".".join(parts)
		print(f'# :simple-python: {parts[-1]}', file=fd)
		print('', file=fd)
		print("::: " + identifier, file=fd)

with mkdocs_gen_files.open("devbook/reference/py/links.md", "w") as nav_file:
	# Exclude the index file from being indexed by the search plugin
	nav_file.writelines(["---\n", "search:\n", "    exclude: true\n", "---\n\n"])
	nav_file.writelines(nav.build_literate_nav())

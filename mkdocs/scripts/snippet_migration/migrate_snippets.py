#!/usr/bin/env python3

# Replace tutorial.code_snippet() macros in md files with tutorial.code_snippet_tagged() macros that use
# section names instead of line numbers. Requires the rannges YAML file obtained with extract_snippet_ranges.py
# CAUTION: calls to tutorial.code_full still need to be manually replaced with tutorial.code_full_tagged,
# but the parameters are the same.
import argparse
import re
from pathlib import Path

import yaml

MACRO_RE = re.compile(
	r"\{\{\s*tutorial\.code_snippet\(\s*\[\s*"
	r"'py:(\d+):(\d+)'\s*,\s*'js:(\d+):(\d+)'\s*"
	r"\]\s*\)\s*\}\}"
)


def load_mapping(yaml_path: Path) -> dict[tuple[str, int, int, int, int], str]:
	data = yaml.safe_load(yaml_path.read_text(encoding="utf-8"))

	mapping = {}

	for item in data:
		page = item["page"].replace("\\", "/")
		index = item["index"]
		snippets = item["snippets"]

		key = (
			page,
			snippets["py"]["start"],
			snippets["py"]["end"],
			snippets["js"]["start"],
			snippets["js"]["end"],
		)

		mapping[key] = f"step-{index}"

	return mapping


def migrate_file(md_path: Path, root: Path, mapping: dict, dry_run: bool) -> bool:
	rel_page = md_path.relative_to(root).as_posix()
	text = md_path.read_text(encoding="utf-8")

	changed = False

	def replace(match: re.Match) -> str:
		nonlocal changed

		py_start, py_end, js_start, js_end = map(int, match.groups())
		key = (rel_page, py_start, py_end, js_start, js_end)

		section_name = mapping.get(key)
		if not section_name:
			return match.group(0)

		changed = True
		return "{{ tutorial.code_snippet_tagged('" + section_name + "') }}"

	new_text = MACRO_RE.sub(replace, text)

	if changed:
		print(f"Updated: {rel_page}")
		if not dry_run:
			md_path.write_text(new_text, encoding="utf-8")

	return changed


def main() -> None:
	parser = argparse.ArgumentParser(
		description="Replace line-number based tutorial snippets with named snippet tags."
	)
	parser.add_argument(
		"folder",
		type=Path,
		help="Folder containing the Markdown files to update.",
	)
	parser.add_argument(
		"mapping",
		type=Path,
		help="YAML file mapping pages and line ranges to snippet indexes.",
	)
	parser.add_argument(
		"--dry-run",
		action="store_true",
		help="Report files that would change without modifying them.",
	)

	args = parser.parse_args()

	root = args.folder.resolve()
	mapping_path = args.mapping.resolve()

	if not root.is_dir():
		raise SystemExit(f"Folder does not exist: {root}")

	if not mapping_path.is_file():
		raise SystemExit(f"YAML file does not exist: {mapping_path}")

	mapping = load_mapping(mapping_path)

	count = 0
	for md_path in root.rglob("*.md"):
		if migrate_file(md_path, root, mapping, args.dry_run):
			count += 1

	print(f"Done. Files changed: {count}" if not args.dry_run else f"Done. Files that would change: {count}")


if __name__ == "__main__":
	main()

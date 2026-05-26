#!/usr/bin/env python3
"""
Scan Markdown files for tutorial.code_snippet([...]) calls and emit YAML.

Expected macro form:

	{{ tutorial.code_snippet(['py:16:18', 'js:12:15']) }}

Output shape:

	- page: devbook/transactions/transfer.md
		snippets:
			py:
				start: 16
				end: 18
			js:
				start: 12
				end: 15

The source file is assumed to match the Markdown file path with the language
extension replacing .md, so it is intentionally omitted.
"""

from __future__ import annotations

import argparse
import ast
import re
from pathlib import Path
from typing import Any

import yaml

MACRO_RE = re.compile(
	r"""
	\{\{\s*
	tutorial\.code_snippet
	\(
		(?P<args>.*?)
	\)
	\s*\}\}
	""",
	re.VERBOSE | re.DOTALL,
)


def parse_snippet_arg(args_text: str, md_path: Path) -> dict[str, dict[str, int]]:
	"""
	Parse the first argument of tutorial.code_snippet(...).

	Supports:

		['py:16:18', 'js:12:15']

	Returns:

		{
			"py": {"start": 16, "end": 18},
			"js": {"start": 12, "end": 15}
		}
	"""
	try:
		value = ast.literal_eval(args_text.strip())
	except Exception as exc:
		raise ValueError(f"{md_path}: cannot parse macro argument: {args_text!r}") from exc

	if not isinstance(value, list):
		raise ValueError(f"{md_path}: expected list argument, got {type(value).__name__}")

	result: dict[str, dict[str, int]] = {}

	for item in value:
		if not isinstance(item, str):
			raise ValueError(f"{md_path}: expected string item, got {item!r}")

		parts = item.split(":")
		if len(parts) != 3:
			raise ValueError(f"{md_path}: expected '<lang>:<start>:<end>', got {item!r}")

		lang, start_text, end_text = parts

		if lang in result:
			raise ValueError(f"{md_path}: duplicate language entry {lang!r}")

		try:
			start = int(start_text)
			end = int(end_text)
		except ValueError as exc:
			raise ValueError(f"{md_path}: invalid line range in {item!r}") from exc

		if start <= 0 or end <= 0:
			raise ValueError(f"{md_path}: line numbers must be positive in {item!r}")

		if end < start:
			raise ValueError(f"{md_path}: end before start in {item!r}")

		result[lang] = {
			"start": start,
			"end": end,
		}

	return result


def scan_markdown_file(md_path: Path, root: Path) -> list[dict[str, Any]]:
	text = md_path.read_text(encoding="utf-8")
	rel_page = md_path.relative_to(root).as_posix()

	records: list[dict[str, Any]] = []

	for index, match in enumerate(MACRO_RE.finditer(text), start=1):
		snippets = parse_snippet_arg(match.group("args"), md_path)

		records.append(
			{
				"page": rel_page,
				"index": index,
				"snippets": snippets,
			}
		)

	return records


def scan_directory(root: Path) -> list[dict[str, Any]]:
	records: list[dict[str, Any]] = []

	for md_path in sorted(root.rglob("*.md")):
		records.extend(scan_markdown_file(md_path, root))

	return records


def main() -> None:
	parser = argparse.ArgumentParser()
	parser.add_argument("directory", type=Path, help="Directory containing Markdown files")
	parser.add_argument("-o", "--output", type=Path, help="Output YAML file")
	args = parser.parse_args()

	root = args.directory.resolve()

	if not root.exists():
		raise SystemExit(f"Directory does not exist: {root}")

	records = scan_directory(root)

	yml = yaml.safe_dump(
		records,
		sort_keys=False,
		allow_unicode=True,
		width=120,
	)

	if args.output:
		args.output.write_text(yml, encoding="utf-8")
	else:
		print(yml, end="")


if __name__ == "__main__":
	main()

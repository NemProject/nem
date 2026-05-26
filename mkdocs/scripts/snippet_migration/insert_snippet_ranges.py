#!/usr/bin/env python3

# Inserts [>section_name] and [<section_name] tags into source code as indicated by the ranges YAML file
# obtained from extract_snippet_ranges.py
from __future__ import annotations

import argparse
import re
from pathlib import Path

import yaml

LANG_EXT = {
	"py": ".py",
	"js": ".mjs",
}

COMMENT_PREFIX = {
	".py": "#",
	".mjs": "//",
}

COMMENT_RE = {
	".py": re.compile(r"# .*$"),
	".mjs": re.compile(r"// .*$"),
}


def source_path(root: Path, page: str, lang: str) -> Path:
	ext = LANG_EXT[lang]
	return root / Path(page).with_suffix(ext)


def is_blank(line: str) -> bool:
	return line.strip() == ""


def add_tag_to_line(line: str, tag: str, comment_prefix: str, comment_re: re.Pattern[str]) -> str:
	newline = "\n" if line.endswith("\n") else ""
	body = line[:-1] if newline else line

	comment_match = comment_re.search(body)

	if comment_match:
		return f"{body} {tag}{newline}"

	if body.strip() == "":
		indent = body
		return f"{indent}{comment_prefix} {tag}{newline}"

	return f"{body} {comment_prefix} {tag}{newline}"


def find_indent(line: str) -> int:
	return len(line) - len(line.lstrip('\t'))


def add_opening_tag(lines: list[str], start_line: int, tag: str, comment_prefix: str, comment_re: re.Pattern[str], indent: int) -> None:
	index = start_line - 1

	if index < 0 or index >= len(lines):
		raise ValueError(f"Start line out of range: {start_line}")

	# If the start line already has a comment, append the tag there.
	if comment_re.search(lines[index]):
		lines[index] = add_tag_to_line(lines[index], tag, comment_prefix, comment_re)
		return

	# Otherwise, prefer the blank line immediately before the start line.
	if index > 0 and is_blank(lines[index - 1]):
		lines[index - 1] = '\t' * indent + add_tag_to_line(lines[index - 1], tag, comment_prefix, comment_re)
		return

	# Fallback: append an inline comment to the start line.
	lines[index] = add_tag_to_line(lines[index], tag, comment_prefix, comment_re)


def add_closing_tag(lines: list[str], end_line: int, tag: str, comment_prefix: str, comment_re: re.Pattern[str], indent: int) -> None:
	index = end_line - 1

	if index < 0 or index >= len(lines):
		raise ValueError(f"End line out of range: {end_line}")

	# Prefer the blank line immediately after the end line.
	if index + 1 < len(lines) and is_blank(lines[index + 1]):
		lines[index + 1] = '\t' * indent + add_tag_to_line(lines[index + 1], tag, comment_prefix, comment_re)
		return

	# Otherwise append an inline comment to the end line.
	lines[index] = add_tag_to_line(lines[index], tag, comment_prefix, comment_re)


def collect_edits(records: list[dict], root: Path) -> dict[Path, list[tuple[int, int, str]]]:
	edits: dict[Path, list[tuple[int, int, str]]] = {}

	for record in records:
		page = record["page"]
		step_index = record["index"]
		step_name = f"step-{step_index}"

		for lang, range_info in record["snippets"].items():
			path = source_path(root, page, lang)

			if lang not in LANG_EXT:
				raise ValueError(f"Unsupported language: {lang}")

			edits.setdefault(path, []).append(
				(
					int(range_info["start"]),
					int(range_info["end"]),
					step_name,
				)
			)

	return edits


def apply_edits(path: Path, ranges: list[tuple[int, int, str]], dry_run: bool) -> None:
	if not path.exists():
		raise FileNotFoundError(path)

	ext = path.suffix
	comment_prefix = COMMENT_PREFIX[ext]
	comment_re = COMMENT_RE[ext]

	lines = path.read_text(encoding="utf-8").splitlines(keepends=True)

	# Apply from bottom to top to avoid line-number drift if future changes insert lines.
	# Current implementation edits existing lines only, but this keeps the script safer.
	for start, end, step_name in sorted(ranges, key=lambda item: (item[1], item[0]), reverse=True):
		indent = find_indent(lines[start - 1])
		add_closing_tag(lines, end, f"[<{step_name}]", comment_prefix, comment_re, indent)
		add_opening_tag(lines, start, f"[>{step_name}]", comment_prefix, comment_re, indent)

	new_text = "".join(lines)

	if dry_run:
		print(f"[dry-run] would update {path}")
	else:
		path.write_text(new_text, encoding="utf-8")
		print(f"updated {path}")


def main() -> None:
	parser = argparse.ArgumentParser()
	parser.add_argument("ranges_yml", type=Path)
	parser.add_argument("source_root", type=Path)
	parser.add_argument("--dry-run", action="store_true")
	args = parser.parse_args()

	records = yaml.safe_load(args.ranges_yml.read_text(encoding="utf-8"))
	root = args.source_root.resolve()

	edits = collect_edits(records, root)

	for path, ranges in sorted(edits.items()):
		apply_edits(path, ranges, args.dry_run)


if __name__ == "__main__":
	main()

import logging
import os
import re
import shutil
import sys
from pathlib import Path

import mkdocs.plugins
import yaml
from mkdocs.structure import files

from mkdocs.config import Config, base

log = logging.getLogger('mkdocs')


def build_nav_order_and_section(config):
	order = {}
	section = {}
	counter = 0

	def walk(items, current_section):
		nonlocal counter

		for item in items:
			if isinstance(item, str):
				order[item] = counter
				section[item] = current_section
				counter += 1

			elif isinstance(item, dict):
				for name, value in item.items():
					if isinstance(value, str):
						order[value] = counter
						section[value] = current_section
						counter += 1
					elif isinstance(value, list):
						walk(value, name)

	walk(config.get("nav", []), 'None')
	return order, section


def parse_page_header(text: str) -> tuple[dict, str | None]:
	"""
	Return the YAML frontmatter of a page and its first level 1 header
	"""
	lines = text.splitlines()
	meta = {}
	start = 0

	if lines and lines[0].strip() == "---":
		for i in range(1, len(lines)):
			if lines[i].strip() == "---":
				raw_yaml = "\n".join(lines[1:i])
				meta = yaml.safe_load(raw_yaml) or {}
				start = i + 1
				break

	for line in lines[start:]:
		stripped = line.strip()
		if stripped.startswith("# "):
			return meta, stripped[2:]

	return meta, None


@mkdocs.plugins.event_priority(-50)
def on_files(in_files: files.Files, config: base.Config) -> files.Files:
	"""
	Exclude from processing files we don't care about:
		Doxygen-generated: We only keep filenames starting with configured prefixes.
	Parse frontmatter of developer tutorials to find their level and store it for later.
	"""
	out_files: list[File] = []
	prefixes = tuple(config["extra"]["nem"]["java-sdk"]["include-prefixes"] + ["links"])
	config['extra']['nem']['tutorials'] = {}
	nav_order, nav_section = build_nav_order_and_section(config)
	section_order = {}
	for f in in_files:
		if f.src_uri.startswith("devbook/reference/java"):
			if not f.name.startswith(prefixes):
				log.debug(f"Custom hook: Removing {f.name}")
				continue
		out_files.append(f)

		if not f.src_path.startswith("devbook/") or not f.src_path.endswith(".md"):
			continue

		src = Path(config["docs_dir"]) / f.src_path
		if not src.is_file():
			continue
		text = src.read_text(encoding="utf-8")
		meta, title = parse_page_header(text)

		if "tutorial_level" not in meta:
			continue

		section = nav_section[f.src_path]
		level = meta["tutorial_level"]
		if section not in config['extra']['nem']['tutorials']:
			config['extra']['nem']['tutorials'][section] = {}
		if level not in config['extra']['nem']['tutorials'][section]:
			config['extra']['nem']['tutorials'][section][level] = []
		config['extra']['nem']['tutorials'][section][level].append({
			"title": meta.get("title") or title,
			"url": '/'.join(f.url.split('/')[1:-1]) + '.md',
			"order": nav_order[f.url[:-1] + '.md']
		})
		section_order[section] = min(section_order.get(section, 999999), nav_order.get(f.src_path, 999999))

	for section in config['extra']['nem']['tutorials'].values():
		for items in section.values():
			items.sort(key=lambda t: t["order"])

	tutorials = config['extra']['nem']['tutorials']
	config['extra']['nem']['tutorials'] = dict(
		sorted(
			tutorials.items(),
			key=lambda item: section_order.get(item[0], 999999),
		)
	)
	return files.Files(out_files)


TAG_RE = re.compile(r"\[(?P<kind>[<>])(?P<name>[A-Za-z0-9_.:-]+)\]")
COMMENT_RE = re.compile(r"(?P<prefix>[ \t]*(#|//))\s*(?P<body>.*)$")


def extract_tutorial_code(config: base.Config) -> None:
	"""
	Scans all .py and .mjs files under snippets/devbook and reads all tutorial code, separating it into
	sections using [>start] and [<end] markers, and removing the markers.
	Stores the result in config.extra.nem.tutorial_code:
	{
		"<path>": {
			"full": str,                # Full source with snippet tags removed (lines preserved)
			"snippets": {
				"<section_name>": {
					"code": str,        # Extracted snippet (tags removed)
					"start_line": int,  # 1-based line number in original file where snippet starts
					"end_line": int     # 1-based line number where snippet ends (exclusive of closing tag)
				},
				...
			}
		},
		...
	}

	Notes:
	- Paths are relative to the `snippets` folder (POSIX-style).
	- Tag-only comment lines are replaced with blank lines in "full.code" to preserve line numbers.
	- Inline tag comments are stripped, preserving the code before the tag.
	"""
	def _comment_contains_only_tags(body: str) -> bool:
		without_tags = TAG_RE.sub("", body).strip()
		return without_tags == ""

	def _remove_tags_from_comment(body: str) -> str:
		return TAG_RE.sub("", body)

	def _process_file(path: Path) -> dict:
		lines = path.read_text(encoding="utf-8").splitlines()

		clean_full_lines: list[str] = []
		snippets: dict[str, dict] = {}

		open_sections: dict[str, dict] = {}

		for index, line in enumerate(lines, start=1):
			comment_match = COMMENT_RE.search(line)
			tags = []

			if comment_match:
				tags = list(TAG_RE.finditer(comment_match.group("body")))

			has_tags = bool(comment_match and tags)
			comment_only_line = has_tags and (line[:comment_match.start()].strip() == "")
			is_tag_only_comment = comment_only_line and _comment_contains_only_tags(comment_match.group("body"))

			clean_line = line

			if has_tags:
				before_comment = line[:comment_match.start()].rstrip()
				after_tags = _remove_tags_from_comment(comment_match.group("body")).strip()

				if before_comment and after_tags:
					clean_line = f"{before_comment} {comment_match.group('prefix')} {after_tags}"
				elif before_comment:
					clean_line = before_comment
				elif after_tags:
					clean_line = f"{line[:comment_match.start()]}{comment_match.group('prefix')} {after_tags}"
				else:
					clean_line = ""

			# First process closing tags on this line.
			#
			# This allows:
			#
			#     // [<first] [>second]
			#
			# to close `first` before opening `second`.
			for tag in tags:
				if tag.group("kind") != "<":
					continue

				name = tag.group("name")

				if name not in open_sections:
					raise ValueError(f"{path}:{index}: closing unopened snippet section '{name}'")

				section = open_sections.pop(name)
				if clean_line:
					section["lines"].append(clean_line)
				snippets[name] = {
					"code": "\n".join(section["lines"]),
					"start_line": section["start_line"],
				}

			# Then process opening tags on this line.
			for tag in tags:
				if tag.group("kind") != ">":
					continue

				name = tag.group("name")

				if name in open_sections:
					raise ValueError(f"{path}:{index}: snippet section '{name}' is already open")

				if name in snippets:
					raise ValueError(f"{path}:{index}: duplicate snippet section '{name}'")

				open_sections[name] = {
					"start_line": index if clean_line else index + 1,
					"lines": [],
				}

			# Remove tag-only comments from rendered full code, but preserve line numbers.
			clean_full_lines.append(clean_line)

			# Marker comments are not included in snippets.
			# If the line had code before the marker comment, keep that code.
			# If it was only a marker comment, preserve the line only in the full listing,
			# not in extracted snippets.
			if clean_line or not has_tags:
				for section in open_sections.values():
					section["lines"].append(clean_line)

		if open_sections:
			still_open = ", ".join(sorted(open_sections))
			raise ValueError(f"{path}: unclosed snippet section(s): {still_open}")

		return {
			"full": "\n".join(clean_full_lines),
			"snippets": snippets,
		}

	root = Path(__file__).parent.parent.joinpath("snippets").resolve()
	examples_dir = root / "devbook"

	config["extra"]["nem"]["tutorial_code"] = {}

	for path in examples_dir.rglob("*"):
		if not path.is_file():
			continue

		if path.suffix not in {".py", ".mjs"}:
			continue

		rel_path = path.relative_to(root).as_posix()
		result = _process_file(path)
		config["extra"]["nem"]["tutorial_code"][rel_path] = result


@mkdocs.plugins.event_priority(50)
def on_pre_build(config: base.Config):
	"""
	Copy the OpenAPI spec file next to its markdown, and load it into the config.
	Load all tutorial sample code into memory and parse sections.
	"""
	spec_path = Path(__file__).parent.parent.parent.joinpath("openapi", "_build").resolve()
	md_path = Path(config.docs_dir).joinpath("devbook", "reference", "rest").resolve()
	spec_fname = 'openapi3.yml'
	shutil.copy2(spec_path / spec_fname, md_path / spec_fname)
	with open(spec_path / spec_fname, 'r', encoding='utf-8') as f:
		config['extra']['nem']['openapi'] = yaml.safe_load(f)
	extract_tutorial_code(config)


def page_markdown_js_typedoc(content, page, config, files):
	"""
	Customize markdown for JS API pages. The Typedoc-markdown plugin does not
	support templates so we need this workaround.
	"""
	if not page.url.startswith("devbook/reference/ts"):
		return content

	symbol_name = ''
	parent_name = page.parent.title if page.parent else ''

	def symbol_type_repl(m):
		dict = {"Class": "class", "Function": "method"}
		nonlocal symbol_name
		symbol_name = m.group(2).removesuffix('()')
		if symbol_name == "default":
			nonlocal parent_name
			symbol_name = parent_name
		# Insert manual word breaks in camel-case titles, in case they are very long
		symbol_name_wbr = re.sub(r'([a-z])([A-Z])', r'\1<wbr>\2', symbol_name)
		if m.group(1) not in dict:
			return f'# {m.group(1)}: {symbol_name_wbr}'
		return f'# :simple-javascript: <code class="doc-symbol doc-symbol-heading doc-symbol-{dict[m.group(1)]}"></code> {symbol_name_wbr}'

	# Add object type icon at the header
	content = re.sub(r'^# ([^:]*): ([^\n]*)', symbol_type_repl, content, count=1)

	# Add glossary definition to page title
	# Documentation MUST NOT start with # so we can tell it apart from the next markdown heading
	content = re.sub(
		r'^(.*?)\n\n([^#].*?)\n\n',
		rf'\1\n\n<dl class="automatic-reference-term" markdown><dt>js:{symbol_name}</dt><dd>\2</dd></dl>\n\n', content, count=1)

	# Add glossary definition to accessors
	# Documentation MUST NOT start with # so we can tell it apart from the next markdown heading
	m = re.search(r'\n## Accessors\n', content)
	if m:
		content = content[:m.start()] + re.sub(
			r"(\n### )([^\n]*?)(\n\n#### Get Signature\n\n```.*?```\n\n)([^#].*?)(\n\n#####)",
			rf'\1\2\3<dl class="automatic-reference-term" markdown><dt>js:{symbol_name}.\2</dt><dd>\4</dd></dl>\5',
			content[m.start():], flags=re.DOTALL)

	# Add glossary definition to methods
	# Documentation MUST NOT start with # so we can tell it apart from the next markdown heading
	m = re.search(r'\n## Methods\n', content)
	if m:
		content = content[:m.start()] + re.sub(
			r"(\n### )([^(]*?)(\(\)\n\n```.*?```\n\n)([^#].*?)(\n\n####)",
			rf'\1\2\3<dl class="automatic-reference-term" markdown><dt>js:{symbol_name}.\2</dt><dd>\4</dd></dl>\5',
			content[m.start():], flags=re.DOTALL)

	# Add glossary definition to global functions
	content = re.sub(
		r"(```ts\nfunction .*?)\n\n(.*?)(\n\n## )",
		rf'<dl class="automatic-reference-term" markdown><dt>js:{symbol_name}</dt><dd>\2</dd></dl>\1\2\3',
		content, flags=re.DOTALL)

	# Add special anchor because the typedoc-md plugin forgot to add it?
	content = re.sub(r'(\n## Constructors)', r'\1<a id="constructor"></a>', content, count=1)

	# Replace \c with code tags
	content = re.sub(r'\\c ([^ ]*?) ', r'`\1` ', content)

	# Remove absolute markdown hyperlinks
	content = re.sub(r'\[([^]]*)\]\(/[^)]*\)', r'\1', content)

	# Remove \note tags. They've been mangled when moved from CATS to JS and are barely usable.
	content = re.sub(r'\\note ', '', content)

	return content


def camel_to_snake(name):
	s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
	return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).lower()


def page_markdown_dylinks(content, page, config, files):
	"""
	The Dynamic Links (dylinks) parser.
	Turn expressions like <dy:ClassName> and <dy:ClassName.methodName> into links to the reference pages that change
	text and href depending on the selected language.
	Accepts JavaScript.camelCase and reformats to Python.snake_case.
	Settings in mkdocs.base.yml:
	- The dictionary extra.nem.class-remaps translates from Python names to JS names, because sometimes they're different.
	- The array extra.nem.global-namespaces lists class names which do not exist in JS and must be removed.
	"""
	langs = ['py', 'js']
	lang_names = ['Python', 'JavaScript']
	class_remaps = config['extra']['nem']['class-remaps']
	global_namespaces = config['extra']['nem']['global-namespaces']
	rgroup_id = 999

	def class_formatter(m):
		nonlocal rgroup_id
		r = '<span markdown class="dylink">'
		for ndx, l in enumerate(langs):
			class_name = m.group(1)
			if l == 'py' and class_name in class_remaps:
				class_name = class_remaps[class_name]
			r += (
				f'<input type="radio" name="rGroup{rgroup_id}" '
				f'id="{lang_names[ndx]}" />'
				f'<label class="dylink-option" for="{lang_names[ndx]}" markdown>'
				f'<{l}:{class_name}></label>'
			)
		r += '</span>'
		rgroup_id += 1
		return r

	def method_formatter(m):
		nonlocal rgroup_id
		r = '<span markdown class="dylink">'
		for ndx, l in enumerate(langs):
			class_name = m.group(1)
			method_name = m.group(2)
			if l == 'py':
				if class_name in class_remaps:
					class_name = class_remaps[class_name]
				method_name = camel_to_snake(method_name)
			if l == 'js' and class_name in global_namespaces:
				class_name = ""
			r += (
				f'<input type="radio" name="rGroup{rgroup_id}" '
				f'id="{lang_names[ndx]}" /><label class="dylink-option" for="{lang_names[ndx]}" markdown>'
				f'<{l}:{class_name}{'.' if class_name else ''}{method_name}></label>'
			)
		r += '</span>'
		rgroup_id += 1
		return r

	content = re.sub(r'<dy:([A-Za-z0-9]*)>', class_formatter, content)
	content = re.sub(r'<dy:([A-Za-z0-9]*)\.([A-Za-z0-9_]*)>', method_formatter, content)

	return content


def page_markdown_rest(content, page, config, files):
	def path_formatter(m):
		method = m.group(1)
		path = m.group(2)
		spec = config['extra']['nem']['openapi']['paths']
		if path not in spec:
			log.warning(f'Page {page.file.src_path} has invalid path {path}')
			return f'**INVALID PATH `{path}`**'
		spec = spec[path]
		if method not in spec:
			log.warning(f'Page {page.file.src_path} has invalid method `{method}` in path {path}')
			return f'**INVALID PATH `{method}:{path}`**'
		spec = spec[method]
		summary = spec['summary']
		r = (
			f'[`{path}`&nbsp;`{method.upper()}`{{.rest-method .rest-method-{method}}}]'
			f'(site:/devbook/reference/rest/nem#tag/{spec['tags'][0].replace(' ', '_')}/{spec['operationId']} "{summary}")'
		)
		return r

	content = re.sub(r'<(get|put|post):([^>]*)>', path_formatter, content)
	return content


def page_markdown_ws(content, page, config, files):
	content = re.sub(r'(<ws:[^>]*>)', r'\1&nbsp;<code class="rest-method rest-method-ws">WS</code>', content)
	return content


def page_markdown_req(content, page, config, files):
	content = re.sub(r'(<req:[^>]*>)', r'\1&nbsp;<code class="rest-method rest-method-req">REQ</code>', content)
	return content


def page_markdown_tutorial_complexity_tags(content, page, config, files):
	if 'tutorial_level' in page.meta:
		level = page.meta['tutorial_level']
		tag = (
			f'<div class="tutorial_level-container">'
			f'<span class="tutorial_level tutorial_level-{level}" title="{config.extra['nem']['tutorial_level']}">'
			f'{config.extra['nem']['tutorial_level_labels'][level]}'
			f'</span>'
			f'</div>'
		)
		content = re.sub(r'(^# [^\n]*)', rf'\g<1>\n\n{tag}', content)
	return content


@mkdocs.plugins.event_priority(0)
def on_page_markdown(content, page, config, files):
	content = page_markdown_js_typedoc(content, page, config, files)
	content = page_markdown_dylinks(content, page, config, files)
	content = page_markdown_rest(content, page, config, files)
	content = page_markdown_ws(content, page, config, files)
	content = page_markdown_req(content, page, config, files)
	content = page_markdown_tutorial_complexity_tags(content, page, config, files)
	return content


class ignoreRESTAnchors(logging.Filter):
	def filter(self, record):
		return not re.search(r'reference/rest/nem.md.*does not contain an anchor', record.msg)


def on_startup(*args, **kwargs):
	"""
	Add the mkdocs folder to PYTHONPATH, so custom modules like the CATS lexer are found.
	Customize the log level of individual plugins.
	"""
	project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
	if project_root not in sys.path:
		sys.path.insert(0, project_root)
	# Make this noisy plugin shut up a bit
	mkdocs.plugins.get_plugin_logger('mkdocs_site_urls').setLevel(logging.WARNING)
	# Silence messages about missing anchors in links to the REST reference guide because that's a dynamic page
	pagesLog = logging.getLogger('mkdocs.structure.pages')
	pagesLog.addFilter(ignoreRESTAnchors())


def on_nav(nav, config, files):
	"""
	Counts the total number of pages on the site, after autogeneration, and stores it for later use.
	"""
	def count_pages(items):
		count = 0
		for item in items:
			if hasattr(item, 'children') and item.children:
				count += count_pages(item.children)
			else:
				count += 1
		return count

	num_pages = count_pages(nav)
	config['extra']['nem']['page_count'] = num_pages
	log.info(f"Custom hook: Counted {num_pages} pages")

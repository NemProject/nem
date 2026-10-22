import posixpath
import re
from pathlib import Path, PurePosixPath
from typing import Optional
from urllib.parse import urlsplit, urlunsplit

import markdown
import mkdocs_gen_files
from bs4 import BeautifulSoup, Comment, NavigableString

nav = mkdocs_gen_files.Nav()

# These Javadoc filenames are useful pages, but their raw stems are poor sidebar labels.
TITLE_OVERRIDES = {
	"package-summary": "PACKAGE SUMMARY",
	"package-tree": "PACKAGE TREE",
}

# Keep package overview pages before the generated class/interface pages in each nav folder.
NAV_ORDER_OVERRIDES = {
	"package-summary": 0,
	"package-tree": 1,
}

VERSIONED_CLASS_NAME = re.compile(
	r"^(?P<base>.+)V(?P<version>\d+)(?P<suffix>Descriptor)?$")
JAVA_CLASS_NAME = re.compile(r"(?<![\w.])[A-Z]\w*(?![\w])")
BACKTICKED_SNAKE_CASE = re.compile(
	r"(?<!`)`([a-z][a-z0-9]*(?:_[a-z0-9]+)+)`(?!`)")
JAVA_AUTOLINK_SKIP_PARENTS = {"a", "code", "pre", "script", "style"}
java_reference_paths = {}


def javadoc_path_to_page_dir(path: PurePosixPath) -> PurePosixPath:
	"""
	Returns the MkDocs directory URL path for a source Javadoc HTML path.
	"""
	# MkDocs directory URLs render Foo.md as Foo/ and index.md as the current folder.
	path = path.with_suffix("")
	if path.name == "index":
		return path.parent
	return path


def nav_parts(parts: tuple[str, ...]) -> tuple[str, ...]:
	"""
	Returns the sidebar path for a generated Java reference page.
	"""
	return (*parts[:-1], TITLE_OVERRIDES.get(parts[-1], parts[-1]))


def source_file_sort_key(path: Path) -> tuple[tuple[str, ...], int, str]:
	"""
	Sorts Javadoc source files in the order they should appear in literate-nav.
	"""
	# Sort per source folder first, then apply local package-page priority.
	rel_path = PurePosixPath(path.relative_to(src_dir).as_posix()).with_suffix("")
	return (
		rel_path.parent.parts,
		NAV_ORDER_OVERRIDES.get(rel_path.name, len(NAV_ORDER_OVERRIDES)),
		rel_path.name,
	)


def rewrite_javadoc_link(current_path: PurePosixPath, href: str) -> str:
	"""
	Rewrites internal Javadoc .html links to MkDocs directory-style links.
	"""
	url = urlsplit(href)
	if url.scheme or url.netloc:
		return href

	redirect = redirect_url(current_path, url)
	if redirect:
		return redirect

	if not url.path.endswith(".html"):
		return href

	# Resolve Javadoc's relative HTML target, then express the equivalent MkDocs page URL
	# relative to the current generated page.
	target_path = PurePosixPath(posixpath.normpath((current_path.parent / url.path).as_posix()))
	return relative_javadoc_url(current_path, target_path, url.query, url.fragment)


def relative_javadoc_url(
	current_path: PurePosixPath,
	target_path: PurePosixPath,
	query: str = "",
	fragment: str = ""
) -> str:
	"""
	Returns the MkDocs directory-style relative URL between two Javadoc pages.
	"""
	current_page_dir = javadoc_path_to_page_dir(current_path)
	target_page_dir = javadoc_path_to_page_dir(target_path)

	if target_page_dir == current_page_dir:
		rewritten_path = "" if fragment else "./"
	else:
		rewritten_path = posixpath.relpath(target_page_dir.as_posix(), current_page_dir.as_posix() or ".")
		rewritten_path = f"{rewritten_path}/"

	return urlunsplit(("", "", rewritten_path, query, fragment))


def relative_docs_url(current_path: PurePosixPath, target_url) -> str:
	"""
	Returns a language-neutral relative link to a docs-root target URL.
	"""
	current_page_dir = dst_dir / javadoc_path_to_page_dir(current_path)
	target_path = PurePosixPath(target_url.path.lstrip("/"))
	if target_path.parts and target_path.parts[0] in languages:
		target_path = PurePosixPath(*target_path.parts[1:])

	rewritten_path = posixpath.relpath(
		target_path.as_posix(), current_page_dir.as_posix() or ".")
	if target_url.path.endswith("/"):
		rewritten_path = f"{rewritten_path}/"

	return urlunsplit(("", "", rewritten_path, target_url.query, target_url.fragment))


def redirect_url(current_path: PurePosixPath, url) -> Optional[str]:
	"""
	Returns the configured replacement for an old docs URL, if one exists.
	"""
	redirect = redirects.get(url.path.lstrip("/"))
	if not redirect:
		return None

	target = urlsplit(redirect)
	query = target.query or url.query
	fragment = target.fragment or url.fragment
	return relative_docs_url(
		current_path,
		target._replace(query=query, fragment=fragment)
	)


def javadoc_link_path(current_path: PurePosixPath, href: str) -> Optional[PurePosixPath]:
	"""
	Resolves a Javadoc link target relative to the current source page.
	"""
	url = urlsplit(href)
	if url.scheme or url.netloc or not url.path.endswith(".html"):
		return None

	return PurePosixPath(posixpath.normpath((current_path.parent / url.path).as_posix()))


def is_ignored_javadoc_path(path: PurePosixPath) -> bool:
	"""
	Returns true when a Javadoc source path points at an ignored Java reference page.
	"""
	parts = path.with_suffix("").parts
	return parts[-1] in ignored_files or any(part in ignored_folders for part in parts)


def remove_adjacent_separator(node) -> None:
	"""
	Removes a comma separator next to an item removed from an inline Javadoc list.
	"""
	for sibling in (node.next_sibling, node.previous_sibling):
		if not isinstance(sibling, str):
			continue

		stripped = sibling.strip()
		if stripped == ",":
			sibling.extract()
			return
		if stripped.startswith(","):
			sibling.replace_with(sibling.replace(",", "", 1))
			return
		if stripped.endswith(","):
			sibling.replace_with(sibling.rsplit(",", 1)[0])
			return


def remove_empty_note_list(node) -> None:
	"""
	Removes a notes definition list if all of its definitions became empty.
	"""
	notes = node.find_parent("dl", class_="notes")
	if not notes:
		return

	if not any(dd.get_text("", strip=True) for dd in notes.find_all("dd")):
		notes.decompose()


def remove_summary_table_row(node) -> bool:
	"""
	Removes a Javadoc summary-table row whose first column references an ignored page.
	"""
	cell = node.find_parent("div", class_=lambda value: value and "col-first" in value.split())
	if not cell:
		return False

	next_cell = cell.find_next_sibling("div", class_=lambda value: value and "col-last" in value.split())
	cell.decompose()
	if next_cell:
		next_cell.decompose()
	return True


def remove_ignored_javadoc_reference(link) -> None:
	"""
	Removes a link to an ignored Java reference page and the smallest useful containing item.
	"""
	if remove_summary_table_row(link):
		return

	item = link.find_parent("code") or link.find_parent("li")
	if not item:
		link.decompose()
		return

	remove_adjacent_separator(item)
	parent = item.parent
	item.decompose()
	remove_empty_note_list(parent)


def remove_ignored_javadoc_references(soup: BeautifulSoup, current_path: PurePosixPath) -> None:
	"""
	Removes references to Java API pages excluded by the ignore-file/folder configuration.
	"""
	for link in list(soup.find_all("a", href=True)):
		if not link.attrs:
			continue
		target_path = javadoc_link_path(current_path, link["href"])
		if target_path and is_ignored_javadoc_path(target_path):
			remove_ignored_javadoc_reference(link)


def short_class_name(name: str) -> str:
	"""
	Returns an unqualified class name from Javadoc's type label text.
	"""
	return name.split("<", 1)[0].rsplit(".", 1)[-1].strip()


def java_glossary_term(name: str) -> str:
	"""
	Returns the ezglossary term key for a Java API symbol.
	"""
	return f"java:{name}"


def remap_class_name(name: str, remaps: dict[str, str]) -> str:
	"""
	Returns the configured glossary name for a Java class.
	"""
	return remaps.get(name, name)


def remap_method_name(class_name: str, method_name: str, remaps: dict[str, str]) -> str:
	"""
	Returns the configured glossary name for a Java method.
	"""
	return remaps.get(f"{class_name}.{method_name}", remaps.get(method_name, method_name))


def build_java_reference_paths(
	html_files: list[Path], root_dir: Path
) -> dict[str, PurePosixPath]:
	"""
	Builds an index of exact and latest-version Java class names.
	"""
	paths = {}
	versioned_paths = {}
	for html_file in html_files:
		indexed_path = PurePosixPath(html_file.relative_to(root_dir).as_posix())
		class_name = html_file.stem.rsplit(".", 1)[-1]
		paths[class_name] = indexed_path
		match = VERSIONED_CLASS_NAME.match(class_name)
		if not match:
			continue

		base_name = match.group("base") + (match.group("suffix") or "")
		version = int(match.group("version"))
		if version > versioned_paths.get(base_name, (0, None))[0]:
			versioned_paths[base_name] = (version, indexed_path)

	for base_name, (_, target_path) in versioned_paths.items():
		paths.setdefault(base_name, target_path)

	return paths


def add_java_reference_links(soup: BeautifulSoup, current_path: PurePosixPath) -> None:
	"""
	Adds links to plain Java class names inside rendered descriptions.
	"""
	descriptions = soup.select(
		"dl.automatic-reference-term > dd, dl.notes dd")
	for text_node in list(
		text for node in descriptions for text in node.find_all(string=True)
	):
		if not isinstance(text_node, NavigableString):
			continue
		if text_node.find_parent(JAVA_AUTOLINK_SKIP_PARENTS):
			continue

		for match in reversed(list(JAVA_CLASS_NAME.finditer(text_node))):
			class_name = match.group(0)
			target_name = (
				f"{class_name}Descriptor"
				if current_path.parent.name == "descriptors"
				else class_name
			)
			target_path = java_reference_paths.get(
				target_name, java_reference_paths.get(class_name))
			if not target_path:
				continue
			if javadoc_path_to_page_dir(target_path) == javadoc_path_to_page_dir(current_path):
				continue

			text = str(text_node)
			link = soup.new_tag(
				"a", href=relative_javadoc_url(current_path, target_path))
			link.string = class_name
			before = NavigableString(text[:match.start()])
			text_node.replace_with(
				before, link, NavigableString(text[match.end():]))
			text_node = before


def convert_backticked_identifier(match: re.Match) -> str:
	"""
	Converts a backticked snake-case identifier to Java lower camel case.
	"""
	first, *remaining = match.group(1).split("_")
	return f"`{first}{''.join(part.capitalize() for part in remaining)}`"


def render_markdown_contents(node) -> None:
	"""
	Renders Markdown-ish text inside a selected Javadoc documentation node.
	"""
	markdown_text = node.decode_contents().replace(
		r"\note ", "<br/>**Note:** ")
	# Restore lists flattened into inline text before Javadoc generated the HTML.
	markdown_text = markdown_text.replace(": - ", ":\n\n- ")
	markdown_text = markdown_text.replace(". - ", ".\n- ")
	rendered_html = markdown.markdown(
		markdown_text, extensions=["md_in_html"])
	rendered_soup = BeautifulSoup(rendered_html, "html.parser")
	node.clear()
	node.extend(rendered_soup.contents)


def render_note_descriptions(soup: BeautifulSoup) -> None:
	"""
	Renders Markdown-ish text in Javadoc notes, such as parameter descriptions.
	"""
	for description_node in soup.select("dl.notes dd"):
		render_markdown_contents(description_node)


def add_class_definition_list(soup: BeautifulSoup) -> None:
	"""
	Wraps the class description in a definition list used by ezglossary.
	"""
	class_description = soup.select_one("section#class-description")
	if not class_description:
		return

	name_node = class_description.select_one(".type-signature .element-name.type-name-label")
	description_node = class_description.select_one(".type-signature + .block")
	if not name_node or not description_node:
		return

	class_name = short_class_name(name_node.get_text("", strip=True))
	render_markdown_contents(description_node)
	definition_list = soup.new_tag("dl", attrs={"class": "automatic-reference-term", "markdown": ""})
	definition_title = soup.new_tag("dt")
	definition_title.string = java_glossary_term(remap_class_name(class_name, class_remaps))
	definition_description = soup.new_tag("dd")

	description_node.replace_with(definition_list)
	definition_list.append(definition_title)
	definition_list.append(definition_description)
	definition_description.append(description_node)


def add_method_definition_lists(soup: BeautifulSoup) -> None:
	"""
	Wraps method descriptions in definition lists used by ezglossary.
	"""
	class_name_node = soup.select_one("section#class-description .type-signature .element-name.type-name-label")
	if not class_name_node:
		return

	class_name = short_class_name(class_name_node.get_text("", strip=True))
	for method in soup.select("section.method-details section.detail"):
		name_node = method.select_one(".member-signature .element-name")
		description_node = method.select_one(".member-signature + .block")
		if not name_node or not description_node:
			continue

		method_name = name_node.get_text("", strip=True)
		remapped_method_name = remap_method_name(class_name, method_name, class_remaps)
		render_markdown_contents(description_node)
		definition_list = soup.new_tag("dl", attrs={"class": "automatic-reference-term", "markdown": ""})
		definition_title = soup.new_tag("dt")
		definition_title.string = java_glossary_term(f"{remap_class_name(class_name, class_remaps)}.{remapped_method_name}")
		definition_description = soup.new_tag("dd")

		description_node.replace_with(definition_list)
		definition_list.append(definition_title)
		definition_list.append(definition_description)
		definition_description.append(description_node)


def transform_javadoc_html(html: str, current_path: PurePosixPath) -> str:
	"""
	Transforms raw Javadoc HTML so it can be embedded inside a MkDocs page.
	"""
	soup = BeautifulSoup(html, "html.parser")

	# Material already provides global navigation, so remove Javadoc's own header chrome.
	for header in soup.select("header.flex-header"):
		header.decompose()

	remove_ignored_javadoc_references(soup, current_path)

	add_class_definition_list(soup)
	add_method_definition_lists(soup)
	render_note_descriptions(soup)
	add_java_reference_links(soup, current_path)

	for link in soup.find_all("a", href=True):
		link["href"] = rewrite_javadoc_link(current_path, link["href"])

	main = soup.find("main")
	if not main:
		raise ValueError(f"Javadoc page does not contain a main element: {current_path}")

	for comment in main.find_all(string=lambda text: isinstance(text, Comment)):
		comment.extract()

	return "".join(str(child) for child in main.contents)


def write_nav_file(output_dir: PurePosixPath) -> None:
	"""
	Writes the generated Java reference navigation file.
	"""
	with mkdocs_gen_files.open(output_dir / "links.md", "w") as nav_file:
		# Exclude the index file from being indexed by the search plugin
		nav_file.writelines([
			"---\n",
			"search:\n",
			"    exclude: true\n",
			"---\n\n"
		])
		nav_file.writelines(nav.build_literate_nav())


config = mkdocs_gen_files.config
project_dir = Path(config.config_file_path).resolve().parent
nem_config = config["extra"]["nem"]
java_sdk_config = nem_config["java-sdk"]
dst_dir = PurePosixPath(java_sdk_config["output_dir"])
src_dir = (project_dir / java_sdk_config["javadoc_build_dir"]).resolve()
ignored_files = java_sdk_config["ignore-files"]
ignored_folders = java_sdk_config["ignore-folders"]
class_remaps = java_sdk_config["class-remaps"]
redirects = {
	redirect["from"].lstrip("/"): redirect["to"]
	for redirect in nem_config.get("redirections", [])
}
languages = {
	alternate["lang"]
	for alternate in config["extra"].get("alternate", [])
}

if java_sdk_config["disabled"]:
	write_nav_file(dst_dir)
else:
	if not src_dir.is_dir():
		raise FileNotFoundError(
			f"Javadoc build directory does not exist: {src_dir}"
		)

	source_files = sorted(
		(
			path for path in src_dir.rglob("*.html")
			if not is_ignored_javadoc_path(
				PurePosixPath(path.relative_to(src_dir).as_posix()))
		),
		key=source_file_sort_key)
	java_reference_paths = build_java_reference_paths(source_files, src_dir)

	for source_file in source_files:
		relative_path = PurePosixPath(source_file.relative_to(src_dir).as_posix())
		# Generated files must use docs-relative POSIX paths for mkdocs-gen-files.
		destination = dst_dir / relative_path.with_suffix(".md")

		src_parts = relative_path.with_suffix("").parts
		full_html = transform_javadoc_html(
			source_file.read_text(encoding="utf-8"), relative_path)
		title = TITLE_OVERRIDES.get(src_parts[-1])

		with mkdocs_gen_files.open(destination, "w") as output:
			print("---", file=output)
			if title:
				print(f'title: "{title}"', file=output)
			print("hide:", file=output)
			print("  - toc", file=output)
			print("---", file=output)
			print(file=output)
			print('<div class="javadoc-container">', file=output)
			print(full_html, file=output)
			print("</div>", file=output)

		mkdocs_gen_files.set_edit_path(destination, source_file.as_posix())
		nav[nav_parts(src_parts)] = relative_path.with_suffix(".md").as_posix()

	write_nav_file(dst_dir)

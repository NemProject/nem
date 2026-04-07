import sys
import os
import logging
import mkdocs.plugins
from mkdocs.structure import files
from mkdocs.config import Config, base
import shutil
from pathlib import Path
import re
import yaml

log = logging.getLogger('mkdocs')

@mkdocs.plugins.event_priority(-50)
def on_files(in_files: files.Files, config: base.Config) -> files.Files:
	"""
	Exclude from processing files we don't care about.
	Doxygen-generated: We only keep filenames starting with configured prefixes.
	"""
	out_files: list[File] = []
	prefixes = tuple(config["extra"]["nem"]["java-sdk"]["include-prefixes"] + ["links"])
	for f in in_files:
		if f.src_uri.startswith("devbook/reference/java"):
			if not f.name.startswith(prefixes):
				log.debug(f"Custom hook: Removing {f.name}")
				continue
		out_files.append(f)

	return files.Files(out_files)

@mkdocs.plugins.event_priority(50)
def on_pre_build(config: base.Config):
	"""
	Copy the OpenAPI spec file next to its markdown, and load it into the config.
	"""
	spec_path = Path(__file__).parent.parent.parent.joinpath("openapi").resolve()
	md_path = Path(config.docs_dir).joinpath("devbook", "reference", "rest").resolve()
	spec_fname = '_build/openapi3.yml'
	shutil.copy2(spec_path / spec_fname, md_path / 'nem-openapi.yml')
	with open(spec_path / spec_fname, 'r', encoding='utf-8') as f:
		config['extra']['nem']['openapi'] = yaml.safe_load(f)

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
		r = f'[`{path}`&nbsp;`{method.upper()}`{{.rest-method .rest-method-{method}}}](site:/devbook/reference/rest/nem#operations-{spec['tags'][0].replace(' ', '_')}-{spec['operationId']} "{summary}")'
		return r

	content = re.sub(r'<(get|put|post):([^>]*)>', path_formatter, content)
	return content

def page_markdown_ws(content, page, config, files):
	content = re.sub(r'(<ws:[^>]*>)', r'\1&nbsp;<code class="rest-method rest-method-ws">WS</code>', content)
	return content

@mkdocs.plugins.event_priority(0)
def on_page_markdown(content, page, config, files):
	content = page_markdown_rest(content, page, config, files)
	content = page_markdown_ws(content, page, config, files)
	return content

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

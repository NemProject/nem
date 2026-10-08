# Author: Jakub Andrýsek
# Email: email@kubaandrysek.cz
# Website: https://kubaandrysek.cz
# License: MIT
# GitHub: https://github.com/JakubAndrysek/mkdocs-typedoc
# PyPI: https://pypi.org/project/mkdocs-typedoc/

import logging
import os
import subprocess

import mkdocs.plugins
from mkdocs.structure.files import File

log: logging.Logger = logging.getLogger("mkdocs")


@mkdocs.plugins.event_priority(10)
def on_files(files, config):
	plugin_config = config["extra"]["nem"]["ts-sdk"]
	# Check if the TypeDoc generation is enabled
	if plugin_config["disabled"]:
		return files

	output_dir = plugin_config["output_dir"]
	# Path to the generated documentation
	doc_path = os.path.join(config["site_dir"], output_dir)

	if not is_typedoc_ready(plugin_config):
		return files

	os.makedirs(doc_path, exist_ok=True)
	if run_typedoc(plugin_config, doc_path):
		add_typedoc_files(files, config, doc_path)

	return files


def is_typedoc_ready(plugin_config):
	# Path to the tsconfig file
	tsconfig_path = plugin_config["tsconfig"]
	if not os.path.exists(tsconfig_path):
		log.error("tsconfig.json file does not exist. Please create it or change the path in mkdocs.yml.")
		return False

	# Check if Node.js is installed
	if not is_node_installed():
		log.error("Node.js is not installed. Please install it from https://nodejs.org/en/download/.")
		return False

	# Check if TypeDoc is installed
	if not is_typedoc_installed():
		log.error(
			"TypeDoc is not installed. Please install it with `npm install typedoc --save-dev`. "
			"See https://typedoc.kubaandrysek.cz for more information."
		)
		return False

	return True


def run_typedoc(plugin_config, doc_path):
	# Build TypeDoc documentation
	try:
		subprocess.run(build_typedoc_command(plugin_config, doc_path), check=True)
		return True
	except subprocess.CalledProcessError as exception:
		log.error("TypeDoc failed with error code %d", exception.returncode)
	except OSError as exception:
		log.error("TypeDoc failed with error: %s", exception)

	return False


def build_typedoc_command(plugin_config, doc_path):
	typedoc_config = [
		("--out", doc_path),
		("--tsconfig", plugin_config["tsconfig"]),
	]

	# Path to the typedoc.json options file
	if plugin_config["options"]:
		typedoc_config.insert(2, ("--options", plugin_config["options"]))

	# Flatten the list of pairs to pass into subprocess.run
	flattened_config = [item for pair in typedoc_config for item in pair]
	return [get_npx_filename(), "typedoc", *flattened_config]


def add_typedoc_files(files, config, doc_path):
	# Add generated TypeDoc documentation to MkDocs
	for dirpath, _dirnames, filenames in os.walk(doc_path):
		for filename in filenames:
			if is_typedoc_summary_file(dirpath, filename):
				continue

			abs_src_path = os.path.join(dirpath, filename)
			doc_rel_path = os.path.relpath(abs_src_path, config["site_dir"])
			files.append(
				File(
					doc_rel_path,
					config["site_dir"],
					config["site_dir"],
					config["use_directory_urls"],
				)
			)


def is_typedoc_summary_file(dirpath, filename):
	stem, extension = os.path.splitext(filename)
	return ".md" == extension and (
		filename.startswith("README") or os.path.isdir(os.path.join(dirpath, stem))
	)


def get_npx_filename():
	return "npx.cmd" if os.name == "nt" else "npx"


def is_node_installed():
	try:
		result = subprocess.run(
			["node", "--version"], check=True, capture_output=True, text=True
		)
		return result.returncode == 0
	except subprocess.CalledProcessError:
		return False
	except OSError as exception:
		log.error("TypeDoc: Node.js failed with error: %s", exception)
		return False


def is_typedoc_installed():
	try:
		result = subprocess.run(
			[get_npx_filename(), "typedoc", "--version"],
			check=True,
			capture_output=True,
			text=True,
		)
		return result.returncode == 0
	except subprocess.CalledProcessError:
		return False
	except OSError as exception:
		log.error("TypeDoc: TypeDoc failed with error: %s", exception)
		return False

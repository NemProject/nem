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
	# Check if the Typedoc generation is enabled
	if plugin_config["disabled"]:
		return files

	# Path to the typedoc.json options file
	typedoc_options = plugin_config["options"]

	output_dir = plugin_config["output_dir"]

	# Path to the generated documentation
	doc_path = os.path.join(config["site_dir"], output_dir)

	if not os.path.exists(doc_path):
		os.makedirs(doc_path)

	# Path to the tsconfig file
	tsconfig_path = plugin_config["tsconfig"]

	if not os.path.exists(tsconfig_path):
		log.error(
			"tsconfig.json file does not exist. Please create it or change the path in mkdocs.yml."
		)
		return files

	# Check if Node.js is installed
	if not is_node_installed():
		log.error(
			"Node.js is not installed. Please install it from https://nodejs.org/en/download/."
		)
		return files

	# Check if TypeDoc is installed
	if not is_typedoc_installed():
		log.error(
			"""TypeDoc is not installed. Please install it with `npm install typedoc --save-dev`.
			See https://typedoc.kubaandrysek.cz for more information."""
		)
		return files

	# Build TypeDoc documentation
	try:
		typedoc_config = [
			("--out", doc_path),
			("--tsconfig", tsconfig_path),
		]

		if typedoc_options:
			typedoc_config.insert(2, ("--options", typedoc_options))

		# Flattening the list of pairs to pass into subprocess.run
		flattened_config = [item for pair in typedoc_config for item in pair]

		command = [get_npx_filename(), "typedoc", *flattened_config]
		subprocess.run(command, check=True)
	except subprocess.CalledProcessError as e:
		log.error("TypeDoc failed with error code %d" % e.returncode)
		return files
	except Exception as e:
		log.error(f"TypeDoc failed with error: {e}")
		return files

	# Add generated TypeDoc documentation to MkDocs
	for dirpath, dirnames, filenames in os.walk(doc_path):
		for filename in filenames:
			if filename.endswith("README.md"):
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

	return files


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
	except Exception as e:
		log.error(f"TypeDoc: Node.js failed with error: {e}")
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
	except Exception as e:
		log.error(f"TypeDoc: TypeDoc failed with error: {e}")
		return False

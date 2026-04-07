set -ex

# Patch the ezglossary plugin, ignoring errors if it was already patched
ez_root=$(pip show mkdocs-ezglossary-plugin | sed -n 's/Location: \(.*\)/\1/p')
ez_plugin=$ez_root/mkdocs_ezglossary_plugin/plugin.py
patch $ez_plugin scripts/ci/ezglossary.patch --force --ignore-whitespace --fuzz 0 || true

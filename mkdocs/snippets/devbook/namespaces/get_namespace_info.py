import json
import os
import urllib.request

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')

NAMESPACE_NAME = os.getenv('NAMESPACE_NAME', 'company')
print(f'Namespace name: {NAMESPACE_NAME}')

try:
	# Fetch namespace information [>step-1]
	namespace_path = f'/namespace?namespace={NAMESPACE_NAME}'
	print(f'Fetching namespace information from {namespace_path}')
	with urllib.request.urlopen(f'{NODE_URL}{namespace_path}') as response:
		namespace_info = json.loads(response.read().decode())
		print('Namespace information:')
		print(f'  Name: {namespace_info["fqn"]}')
		print(f'  Owner: {namespace_info["owner"]}')
		lease_height = namespace_info['height']
		print(f'  Height: {lease_height}')
	# [<step-1]
	# Compute the lease expiration [>step-2]
	LEASE_DURATION = 525600  # approximately one year of blocks
	with urllib.request.urlopen(f'{NODE_URL}/chain/height') as response:
		current_height = json.loads(response.read().decode())['height']
	expiration_height = lease_height + LEASE_DURATION
	print(f'\nCurrent chain height: {current_height}')
	print(f'Lease expiration height: {expiration_height}')
	print(f'Blocks until expiration: {expiration_height - current_height}')
	# [<step-2]
	# List the subnamespaces [>step-3]
	owner = namespace_info['owner']
	subnamespaces_path = (
		f'/account/namespace/page'
		f'?address={owner}&parent={NAMESPACE_NAME}')
	print(f'\nFetching subnamespaces from {subnamespaces_path}')
	with urllib.request.urlopen(
		f'{NODE_URL}{subnamespaces_path}'
	) as response:
		subnamespaces = json.loads(response.read().decode())['data']
		print(f'Subnamespaces of {NAMESPACE_NAME}: {len(subnamespaces)}')
		for subnamespace in subnamespaces:
			print(f'  {subnamespace["fqn"]}')
	# [<step-3]
	# List the mosaics defined under the namespace [>step-4]
	mosaics_path = (
		f'/namespace/mosaic/definition/page?namespace={NAMESPACE_NAME}')
	print(f'\nFetching mosaic definitions from {mosaics_path}')
	with urllib.request.urlopen(f'{NODE_URL}{mosaics_path}') as response:
		mosaics = json.loads(response.read().decode())['data']
		print(f'Mosaics defined under {NAMESPACE_NAME}: {len(mosaics)}')
		for entry in mosaics:
			mosaic_id = entry['mosaic']['id']
			print(f'  {mosaic_id["namespaceId"]}:{mosaic_id["name"]}')
	# [<step-4]
except Exception as e:
	print(e)

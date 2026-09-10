import json
import os
import urllib.request

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')

MOSAIC_ID = os.getenv('MOSAIC_ID', 'nem:xem')
print(f'Mosaic ID: {MOSAIC_ID}')

try:
	# Fetch mosaic information [>step-1]
	mosaic_path = f'/mosaic/definition?mosaicId={MOSAIC_ID}'
	print(f'Fetching mosaic information from {mosaic_path}')
	with urllib.request.urlopen(f'{NODE_URL}{mosaic_path}') as response:
		response_json = json.loads(response.read().decode())
		mosaic_id = response_json['id']
		full_name = f'{mosaic_id["namespaceId"]}:{mosaic_id["name"]}'
		print('Mosaic information:')
		print(f'  Mosaic ID: {full_name}')
		print(f'  Description: {response_json["description"]}')
		print(f'  Creator: {response_json["creator"]}')
		properties = {
			prop['name']: prop['value']
			for prop in response_json['properties']
		}
		divisibility = int(properties['divisibility'])
		print(f'  Divisibility: {divisibility}')
		print(f'  Initial supply: {properties["initialSupply"]}')
		print(f'  Supply mutable: {properties["supplyMutable"]}')
		print(f'  Transferable: {properties["transferable"]}')
		levy = response_json['levy']
		print(f'  Levy: {levy if levy else "none"}')
	# [<step-1]
	# Fetch the current supply [>step-2]
	supply_path = f'/mosaic/supply?mosaicId={MOSAIC_ID}'
	print(f'\nFetching current supply from {supply_path}')
	with urllib.request.urlopen(f'{NODE_URL}{supply_path}') as response:
		supply_info = json.loads(response.read().decode())
		supply = supply_info['supply']
		print(f'  Current supply: {supply}')
	# [<step-2]
	# Convert the supply to atomic units [>step-3]
	atomic = supply * 10 ** divisibility
	print(f'\nSupply in atomic units: {atomic}')
	# [<step-3]
except Exception as e:
	print(e)

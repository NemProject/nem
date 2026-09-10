import json
import os
import urllib.request

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')


def get_mosaic_balances(address):  # [>step-2]
	"""
	Fetch all mosaic balances owned by an account.

	Args:
		address: The account address

	Returns:
		List of mosaics, each with a structured mosaicId and quantity
	"""
	balances_path = f'/account/mosaic/owned?address={address}'
	with urllib.request.urlopen(f'{NODE_URL}{balances_path}') as response:
		balances_info = json.loads(response.read().decode())
		return balances_info['data']  # [<step-2]


def get_mosaic_definitions(address):  # [>step-3]
	"""
	Fetch mosaic definitions for every mosaic owned by an account.

	Args:
		address: The account address

	Returns:
		Dictionary mapping "namespace:name" to the mosaic definition
	"""
	definitions_path = '/account/mosaic/owned/definition'
	with urllib.request.urlopen(
		f'{NODE_URL}{definitions_path}?address={address}'
	) as response:
		definitions_info = json.loads(response.read().decode())
		# Build a dictionary mapping "namespace:name" to its definition
		definitions_map = {}
		for entry in definitions_info['data']:
			entry_id = entry['id']
			entry_key = f'{entry_id["namespaceId"]}:{entry_id["name"]}'
			definitions_map[entry_key] = entry
		return definitions_map  # [<step-3]


def format_amount(amount, divisibility):  # [>step-4]
	"""
	Format an atomic amount with decimal places.

	Args:
		amount: The atomic amount as an integer
		divisibility: Number of decimal places

	Returns:
		Formatted amount as a string
	"""
	if divisibility == 0:
		return str(amount)
	whole_part = amount // (10 ** divisibility)
	fractional_part = amount % (10 ** divisibility)
	return f'{whole_part}.{fractional_part:0{divisibility}d}'  # [<step-4]


# The account address to query [>step-5]
ADDRESS = os.getenv('ADDRESS', 'TBONKWCOWBZYZB2I5JD3LSDBQVBYHB757VN3SKPP')
print(f'Fetching balances for {ADDRESS}')

try:
	# Fetch mosaic balances and definitions for the account
	account_mosaics = get_mosaic_balances(ADDRESS)
	mosaic_definitions = get_mosaic_definitions(ADDRESS)

	if not account_mosaics:
		print('Account holds no mosaics')
	else:
		print(f'Account holds {len(account_mosaics)} mosaic(s):')

		for mosaic_entry in account_mosaics:
			mosaic_id = mosaic_entry['mosaicId']
			key = f'{mosaic_id["namespaceId"]}:{mosaic_id["name"]}'
			balance = int(mosaic_entry['quantity'])

			# Get mosaic divisibility from the definition
			definition = mosaic_definitions[key]
			properties = {
				p['name']: p['value']
				for p in definition['properties']
			}
			mosaic_divisibility = int(
				properties.get('divisibility', '0'))

			# Format and display the balance
			formatted_balance = format_amount(
				balance, mosaic_divisibility)
			print(f'- Mosaic {key}')
			print(f'  Balance: {formatted_balance}')
			print(f'  Balance (atomic): {balance}')
			print(f'  Divisibility: {mosaic_divisibility}')
except urllib.error.URLError as e:
	print(e.reason)  # [<step-5]

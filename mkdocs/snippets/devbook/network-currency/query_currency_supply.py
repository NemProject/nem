import json
import os
import urllib.request

NODE_URL = os.getenv('NODE_URL', 'http://portobelo.nemmain.net:7890')
print(f'Using node {NODE_URL}')

try:
	MOSAIC_ID = 'nem:xem'  # [>step-1]
	supply_path = f'/mosaic/supply?mosaicId={MOSAIC_ID}'
	with urllib.request.urlopen(f'{NODE_URL}{supply_path}') as response:
		supply_info = json.loads(response.read().decode())
	total_supply = supply_info['supply']
	print(f'Total supply: {total_supply:,.6f} {MOSAIC_ID}')  # [<step-1]
	# Read the mosaic's divisibility to convert balances to whole units [>step-2]
	definition_path = f'/mosaic/definition?mosaicId={MOSAIC_ID}'
	with urllib.request.urlopen(
		f'{NODE_URL}{definition_path}'
	) as response:
		definition = json.loads(response.read().decode())
	properties = {
		prop['name']: prop['value']
		for prop in definition['properties']
	}
	divisibility = int(properties['divisibility'])
	# [<step-2]
	# [>step-3]
	NON_CIRCULATING_ADDRESSES = [
		('Treasury', 'NCHESTYVD2P6P646AMY7WSNG73PCPZDUQNSD6JAK'),
		('Nemesis', 'NANEMOABLAGR72AZ2RV3V4ZHDCXW25XQ73O7OBT5'),
		('Namespace rental', 'NAMESPACEWH4MKFMBCVFERDPOOP4FK7MTBXDPZZA'),
		('Mosaic rental', 'NBMOSAICOD4F54EE5CDMR23CCBGOAM2XSIUX6TRS'),
	]
	non_circulating_supply = 0
	for label, address in NON_CIRCULATING_ADDRESSES:
		account_path = f'/account/get?address={address}'
		with urllib.request.urlopen(
			f'{NODE_URL}{account_path}'
		) as response:
			account_info = json.loads(response.read().decode())
		balance = (account_info['account']['balance']
			/ (10 ** divisibility))
		non_circulating_supply += balance
		print(f'  {label}: {balance:,.6f} {MOSAIC_ID}')
	print(
		f'Non-circulating supply: '
		f'{non_circulating_supply:,.6f} {MOSAIC_ID}')  # [<step-3]
	# [>step-4]
	circulating_supply = total_supply - non_circulating_supply
	print(f'Circulating supply: {circulating_supply:,.6f} {MOSAIC_ID}')  # [<step-4]
except Exception as error:
	print(error)

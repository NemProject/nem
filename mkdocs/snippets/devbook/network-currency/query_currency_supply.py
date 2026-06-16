import json
import os
import urllib.request

NODE_URL = os.getenv('NODE_URL', 'http://portobelo.nemmain.net:7890')
print(f'Using node {NODE_URL}')

try:
	supply_path = '/mosaic/supply?mosaicId=nem:xem' # [>step-1]
	with urllib.request.urlopen(f'{NODE_URL}{supply_path}') as response:
		supply_info = json.loads(response.read().decode())
	total = supply_info['supply']
	print(f'Total supply: {total:,.6f} XEM')  # [<step-1]
	# [>step-2]
	NON_CIRCULATING_ADDRESSES = [
		('Treasury', 'NCHESTYVD2P6P646AMY7WSNG73PCPZDUQNSD6JAK'),
		('Nemesis', 'NANEMOABLAGR72AZ2RV3V4ZHDCXW25XQ73O7OBT5'),
		('Namespace rental', 'NAMESPACEWH4MKFMBCVFERDPOOP4FK7MTBXDPZZA'),
		('Mosaic rental', 'NBMOSAICOD4F54EE5CDMR23CCBGOAM2XSIUX6TRS'),
	]
	non_circulating = 0
	for label, address in NON_CIRCULATING_ADDRESSES:
		account_path = f'/account/get?address={address}'
		with urllib.request.urlopen(
			f'{NODE_URL}{account_path}'
		) as response:
			account_info = json.loads(response.read().decode())
		balance = account_info['account']['balance'] / 1_000_000
		non_circulating += balance
		print(f'  {label}: {balance:,.6f} XEM')
	print(f'Non-circulating supply: {non_circulating:,.6f} XEM')  # [<step-2]

	# [>step-3]
	circulating = total - non_circulating
	print(f'Circulating supply: {circulating:,.6f} XEM')  # [<step-3]
except Exception as error:
	print(error)

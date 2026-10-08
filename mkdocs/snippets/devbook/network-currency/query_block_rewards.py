import json
import os
import urllib.request

from symbolchain.CryptoTypes import PublicKey
from symbolchain.facade.NemFacade import NemFacade

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')

BLOCK_HEIGHT = os.getenv('BLOCK_HEIGHT', '661258')

facade = NemFacade('testnet')

try:
	# Fetch the block at the given height [>step-1]
	block_url = f'{NODE_URL}/block/at/public'
	request = urllib.request.Request(
		block_url,
		data=json.dumps({'height': int(BLOCK_HEIGHT)}).encode(),
		headers={'Content-Type': 'application/json'})
	with urllib.request.urlopen(request) as response:
		block = json.loads(response.read())
	transactions = block['transactions']
	print(f'Block height: {BLOCK_HEIGHT}')
	print(f'Transactions: {len(transactions)}')
	# [<step-1]
	# Identify the harvester [>step-2]
	harvester = facade.network.public_key_to_address(
		PublicKey(block['signer']))
	print(f'Harvester: {harvester}')
	# [<step-2]
	# Sum the transaction fees [>step-3]
	total_reward = 0
	print('\nTransaction fees:')
	for transaction in transactions:
		fee = int(transaction['fee'])
		total_reward += fee
		print(f'  Fee: {fee / 1e6:,.6f} XEM')
	# [<step-3]
	# Total reward [>step-4]
	print(f'\nTotal block reward: {total_reward / 1e6:,.6f} XEM')
	# [<step-4]
except Exception as error:
	print(error)

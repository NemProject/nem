import json
import os
import time
import urllib.request

from symbolchain.CryptoTypes import PrivateKey
from symbolchain.facade.NemFacade import NemFacade

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')

print(f'Using node {NODE_URL}')
# [>step-1]
SIGNER_PRIVATE_KEY = os.getenv(
	'SIGNER_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000000')
signer_key_pair = NemFacade.KeyPair(PrivateKey(SIGNER_PRIVATE_KEY))
RECIPIENT_ADDRESS = os.getenv(
	'RECIPIENT_ADDRESS',
	'TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4')
# [<step-1]
facade = NemFacade('testnet')
# Define the amount of XEM to transfer [>step-2]
xem = int(os.getenv('XEM_AMOUNT', '1'))
# Convert XEM to atomic units (XEM has a divisibility of 6)
amount = xem * 1_000_000
# [<step-2]

try:
	# Fetch current network time [>step-3]
	time_path = '/time-sync/network-time'
	print(f'Fetching current network time from {time_path}')
	with urllib.request.urlopen(f'{NODE_URL}{time_path}') as response:
		response_json = json.loads(response.read().decode())
		network_time = response_json['receiveTimeStamp'] // 1000
		print(f'  Network time: {network_time} s since the nemesis block')

	# Derived fields from network time
	timestamp = network_time
	deadline = network_time + 2 * 60 * 60
	# [<step-3]
	# Calculate the transaction fee [>step-4]
	fee_steps = max(1, min(25, xem // 10_000))
	fee = fee_steps * 50_000
	print(f'  Transaction fee: {fee / 1_000_000} XEM')
	# [<step-4]
	# Build the transaction [>step-5]
	transaction = facade.transaction_factory.create({
		'type': 'transfer_transaction_v2',
		'signer_public_key': signer_key_pair.public_key,
		'fee': fee,
		'timestamp': timestamp,
		'deadline': deadline,
		'recipient_address': RECIPIENT_ADDRESS,
		'amount': amount
	})
	# [<step-5]
	# Sign transaction and generate final payload [>step-6]
	signature = facade.sign_transaction(signer_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	print('Built transaction:')
	print(json.dumps(transaction.to_json(), indent=2))
	# [<step-6]
	# Announce the transaction [>step-7]
	announce_path = '/transaction/announce'
	print(f'Announcing transaction to {announce_path}')
	announce_request = urllib.request.Request(
		f'{NODE_URL}{announce_path}',
		data=json_payload.encode(),
		headers={'Content-Type': 'application/json'},
		method='POST'
	)
	with urllib.request.urlopen(announce_request) as response:
		announce_result = json.loads(response.read().decode())
	print(f'  Result: {announce_result['message']}')
	# [<step-7]
	# Wait for confirmation [>step-8]
	if 'SUCCESS' == announce_result['message']:
		status_path = (
			f'/transaction/get?hash={
				facade.hash_transaction(transaction)}')
		print(f'Waiting for confirmation from {status_path}')
		is_confirmed = False
		for attempt in range(120):
			try:
				with urllib.request.urlopen(
					f'{NODE_URL}{status_path}'
				) as response:
					confirmed = json.loads(response.read().decode())
					height = confirmed['meta']['height']
					print(f'Transaction confirmed in block {height}')
					is_confirmed = True
					break
			except urllib.error.HTTPError:
				print('  Transaction status: pending')
			time.sleep(1)
		if not is_confirmed:
			print('Confirmation took too long.')
	else:
		print(f'Transaction rejected: {announce_result['message']}')
	# [<step-8]
except urllib.error.URLError as e:
	print(e.reason)

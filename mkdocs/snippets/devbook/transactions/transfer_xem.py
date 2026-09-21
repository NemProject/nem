import json
import os
import time
import urllib.request

from symbolchain.CryptoTypes import PrivateKey
from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nc import Amount
from symbolchain.nem.FeeCalculator import calculate_transaction_fee

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
xem = float(os.getenv('XEM_AMOUNT', '1'))
amount = round(xem * 1_000_000)
# [<step-2]

try:
	# Build the transaction [>step-3]
	transaction = facade.create_transaction_from_descriptor({
		'type': 'transfer_transaction_v2',
		'recipient_address': RECIPIENT_ADDRESS,
		'amount': amount
	}, signer_key_pair.public_key, 0, 2 * 60 * 60)
	# [<step-3]
	# Calculate and attach the transaction fee [>step-4]
	fee = calculate_transaction_fee(transaction)
	transaction.fee = Amount(fee)
	print(f'  Transaction fee: {fee / 1_000_000} XEM')
	# [<step-4]
	# Sign transaction and generate final payload [>step-5]
	signature = facade.sign_transaction(signer_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	print('Built transaction:')
	print(json.dumps(transaction.to_json(), indent=2))
	# [<step-5]
	# Announce the transaction [>step-6]
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
	# [<step-6]
	# Wait for confirmation [>step-7]
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
	# [<step-7]
except urllib.error.URLError as e:
	print(e.reason)

import json
import os
import time
import urllib.request

from symbolchain.CryptoTypes import PrivateKey
from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nc import Amount
from symbolchain.nem.FeeCalculator import (
	calculate_mosaic_rental_fee,
	calculate_transaction_fee
)

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')


# Helper function to announce a transaction
def announce_transaction(payload, label):
	announce_path = '/transaction/announce'
	print(f'Announcing {label} to {announce_path}')
	request = urllib.request.Request(
		f'{NODE_URL}{announce_path}',
		data=payload.encode(),
		headers={'Content-Type': 'application/json'},
		method='POST'
	)
	with urllib.request.urlopen(request) as announce_response:
		result = json.loads(announce_response.read().decode())
	print(f'  Result: {result["message"]}')
	return result['message']


# Helper function to wait for transaction confirmation
def wait_for_confirmation(tx_hash, label):
	status_path = f'/transaction/get?hash={tx_hash}'
	print(f'Waiting for {label} confirmation from {status_path}')
	is_confirmed = False
	for _ in range(120):
		try:
			with urllib.request.urlopen(
				f'{NODE_URL}{status_path}'
			) as status_response:
				confirmed = json.loads(status_response.read().decode())
				height = confirmed['meta']['height']
				print(f'{label} confirmed in block {height}')
				is_confirmed = True
				break
		except urllib.error.HTTPError:
			print('  Transaction status: pending')
		time.sleep(1)
	if not is_confirmed:
		print(f'{label} confirmation took too long.')


# [>step-1]
SIGNER_PRIVATE_KEY = os.getenv(
	'SIGNER_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000000')
signer_key_pair = NemFacade.KeyPair(PrivateKey(SIGNER_PRIVATE_KEY))

facade = NemFacade('testnet')
signer_address = facade.network.public_key_to_address(
	signer_key_pair.public_key)
print(f'Signer address: {signer_address}')

namespace_name = os.getenv('NAMESPACE', 'my_namespace')
mosaic_name = os.getenv('MOSAIC', f'token_{int(time.time())}')
mosaic_id = f'{namespace_name}:{mosaic_name}'
print(f'Creating mosaic: {mosaic_id}')
# [<step-1]
try:
	# Describe the levy [>step-2]
	LEVY_RECIPIENT = os.getenv(
		'LEVY_RECIPIENT',
		'TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4')

	levy = {
		'transfer_fee_type': 'absolute',
		'recipient_address': LEVY_RECIPIENT,
		'mosaic_id': {
			'namespace_id': {'name': 'nem'},
			'name': 'xem'
		},
		'fee': 1_000_000
	}
	levy_mosaic_id = levy['mosaic_id']
	print('Levy:')
	print(f'  Type: {levy["transfer_fee_type"]}')
	print(f'  Recipient: {levy["recipient_address"]}')
	print(f'  Mosaic: {levy_mosaic_id["namespace_id"]["name"]}:'
		f'{levy_mosaic_id["name"]}')
	print(f'  Fee: {levy["fee"]}')
	# [<step-2]
	# Build the mosaic definition transaction [>step-3]
	rental_fee = calculate_mosaic_rental_fee()
	print(f'  Mosaic creation fee: {rental_fee / 1_000_000} XEM')

	transaction = facade.create_transaction_from_descriptor({
		'type': 'mosaic_definition_transaction_v1',
		'rental_fee_sink': 'TBMOSAICOD4F54EE5CDMR23CCBGOAM2XSJBR5OLC',
		'rental_fee': rental_fee,
		'mosaic_definition': {
			'owner_public_key': signer_key_pair.public_key,
			'id': {
				'namespace_id': {'name': namespace_name},
				'name': mosaic_name
			},
			'description': 'My tutorial mosaic with a levy',
			'properties': [
				{'property_': {
					'name': b'divisibility', 'value': b'2'}},
				{'property_': {
					'name': b'initialSupply', 'value': b'1000'}},
				{'property_': {
					'name': b'supplyMutable', 'value': b'true'}},
				{'property_': {
					'name': b'transferable', 'value': b'true'}}
			],
			'levy': levy
		}
	}, signer_key_pair.public_key, 0, 2 * 60 * 60)

	# Calculate and attach the transaction fee
	fee = calculate_transaction_fee(transaction)
	transaction.fee = Amount(fee)
	print(f'  Transaction fee: {fee / 1_000_000} XEM')
	# [<step-3]
	# Sign, announce and wait for confirmation [>step-4]
	signature = facade.sign_transaction(signer_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	print('Built mosaic definition transaction:')
	print(json.dumps(transaction.to_json(), indent=2))

	transaction_hash = facade.hash_transaction(transaction)
	print(f'Transaction hash: {transaction_hash}')
	announce_result = announce_transaction(
		json_payload, 'mosaic definition')
	if 'SUCCESS' == announce_result:
		wait_for_confirmation(transaction_hash, 'mosaic definition')
	else:
		print(f'Transaction rejected: {announce_result}')
	# [<step-4]
	# Retrieve the levy [>step-5]
	definition_path = f'/mosaic/definition?mosaicId={mosaic_id}'
	print(f'Fetching mosaic information from {definition_path}')
	with urllib.request.urlopen(
		f'{NODE_URL}{definition_path}'
	) as response:
		mosaic_info = json.loads(response.read().decode())
		levy_info = mosaic_info['levy']
		levy_mosaic_id = levy_info['mosaicId']
		levy_type = 'absolute' if 1 == levy_info['type'] else 'percentile'
		print('Levy information:')
		print(f'  Type: {levy_type}')
		print(f'  Recipient: {levy_info["recipient"]}')
		print(f'  Mosaic: '
			f'{levy_mosaic_id["namespaceId"]}:{levy_mosaic_id["name"]}')
		print(f'  Fee: {levy_info["fee"]}')
	# [<step-5]
except Exception as error:
	print(error)

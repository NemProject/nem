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
from symbolchain.nem.Network import NetworkTimestamp

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')
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
	# Fetch current network time [>step-2]
	time_path = '/time-sync/network-time'
	print(f'Fetching current network time from {time_path}')
	with urllib.request.urlopen(f'{NODE_URL}{time_path}') as response:
		response_json = json.loads(response.read().decode())
		network_time = response_json['receiveTimeStamp'] // 1000
		print(f'  Network time: {network_time} s since the nemesis block')

	# Derived fields from network time
	timestamp = NetworkTimestamp(network_time)
	deadline = timestamp.add_hours(2)
	# [<step-2]
	# Describe the levy [>step-3]
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
	# [<step-3]
	# Build the mosaic definition transaction [>step-4]
	rental_fee = calculate_mosaic_rental_fee()
	print(f'  Mosaic creation fee: {rental_fee / 1_000_000} XEM')

	transaction = facade.transaction_factory.create({
		'type': 'mosaic_definition_transaction_v1',
		'signer_public_key': signer_key_pair.public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
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
	})

	# Calculate and attach the transaction fee
	fee = calculate_transaction_fee(transaction)
	transaction.fee = Amount(fee)
	print(f'  Transaction fee: {fee / 1_000_000} XEM')
	# [<step-4]
	# Sign, announce and wait for confirmation [>step-5]
	signature = facade.sign_transaction(signer_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	print('Built mosaic definition transaction:')
	print(json.dumps(transaction.to_json(), indent=2))

	announce_path = '/transaction/announce'
	print(f'Announcing mosaic definition to {announce_path}')
	announce_request = urllib.request.Request(
		f'{NODE_URL}{announce_path}',
		data=json_payload.encode(),
		headers={'Content-Type': 'application/json'},
		method='POST'
	)
	with urllib.request.urlopen(announce_request) as response:
		announce_result = json.loads(response.read().decode())
	print(f'  Result: {announce_result["message"]}')

	if 'SUCCESS' == announce_result['message']:
		transaction_hash = facade.hash_transaction(transaction)
		status_path = f'/transaction/get?hash={transaction_hash}'
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
		print(f'Transaction rejected: {announce_result["message"]}')
	# [<step-5]
	# Retrieve the levy [>step-6]
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
	# [<step-6]
except urllib.error.URLError as e:
	print(e.reason)

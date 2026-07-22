import json
import os
import time
import urllib.request

from symbolchain.CryptoTypes import PrivateKey
from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nc import Amount
from symbolchain.nem.FeeCalculator import calculate_transaction_fee
from symbolchain.nem.Network import NetworkTimestamp

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


# Helper function to fetch the current mosaic supply
def fetch_supply(mosaic):
	supply_path = f'/mosaic/supply?mosaicId={mosaic}'
	with urllib.request.urlopen(
		f'{NODE_URL}{supply_path}'
	) as supply_response:
		supply_info = json.loads(supply_response.read().decode())
	return supply_info['supply']


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


SIGNER_PRIVATE_KEY = os.getenv(  # [>step-1]
	'SIGNER_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000000')
signer_key_pair = NemFacade.KeyPair(PrivateKey(SIGNER_PRIVATE_KEY))

facade = NemFacade('testnet')
signer_address = facade.network.public_key_to_address(
	signer_key_pair.public_key)
print(f'Signer address: {signer_address}')

namespace_name = os.getenv('NAMESPACE', 'my_namespace')
mosaic_name = os.getenv('MOSAIC', 'token')
mosaic_id = f'{namespace_name}:{mosaic_name}'
print(f'Mosaic ID: {mosaic_id}')
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
	# --- INCREASING SUPPLY (MINTING) ---
	print('\n--- Increasing supply (minting) ---')
	# [>step-3]
	print(f'Supply before minting: {fetch_supply(mosaic_id)}')

	increase_tx = facade.transaction_factory.create({
		'type': 'mosaic_supply_change_transaction_v1',
		'signer_public_key': signer_key_pair.public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
		'mosaic_id': {
			'namespace_id': {'name': namespace_name},
			'name': mosaic_name
		},
		'action': 'increase',
		'delta': 500
	})
	increase_tx.fee = Amount(calculate_transaction_fee(increase_tx))

	signature = facade.sign_transaction(signer_key_pair, increase_tx)
	json_payload = facade.transaction_factory.attach_signature(
		increase_tx, signature)
	print('Built supply increase transaction:')
	print(json.dumps(increase_tx.to_json(), indent=2))
	if 'SUCCESS' == announce_transaction(json_payload, 'supply increase'):
		wait_for_confirmation(
			facade.hash_transaction(increase_tx), 'supply increase')
		print(f'Supply after minting: {fetch_supply(mosaic_id)}')
	else:
		print('Supply increase rejected')
	# [<step-3]
	# --- DECREASING SUPPLY (BURNING) ---
	print('\n--- Decreasing supply (burning) ---')
	# [>step-4]
	decrease_tx = facade.transaction_factory.create({
		'type': 'mosaic_supply_change_transaction_v1',
		'signer_public_key': signer_key_pair.public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
		'mosaic_id': {
			'namespace_id': {'name': namespace_name},
			'name': mosaic_name
		},
		'action': 'decrease',
		'delta': 500
	})
	decrease_tx.fee = Amount(calculate_transaction_fee(decrease_tx))

	signature = facade.sign_transaction(signer_key_pair, decrease_tx)
	json_payload = facade.transaction_factory.attach_signature(
		decrease_tx, signature)
	print('Built supply decrease transaction:')
	print(json.dumps(decrease_tx.to_json(), indent=2))
	if 'SUCCESS' == announce_transaction(json_payload, 'supply decrease'):
		wait_for_confirmation(
			facade.hash_transaction(decrease_tx), 'supply decrease')
		print(f'Supply after burning: {fetch_supply(mosaic_id)}')
	else:
		print('Supply decrease rejected')
	# [<step-4]
except urllib.error.URLError as e:
	print(e.reason)

import json
import os
import time
import urllib.request

from symbolchain.CryptoTypes import PrivateKey
from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nc import Amount
from symbolchain.nem.FeeCalculator import (
	calculate_namespace_rental_fee,
	calculate_transaction_fee
)

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')

SIGNER_PRIVATE_KEY = os.getenv(
	'SIGNER_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000000')
signer_key_pair = NemFacade.KeyPair(PrivateKey(SIGNER_PRIVATE_KEY))

facade = NemFacade('testnet')
signer_address = facade.network.public_key_to_address(
	signer_key_pair.public_key)
print(f'Signer address: {signer_address}')

try:
	# Fetch current network time
	time_path = '/time-sync/network-time'
	print(f'Fetching current network time from {time_path}')
	with urllib.request.urlopen(f'{NODE_URL}{time_path}') as response:
		response_json = json.loads(response.read().decode())
		network_time = response_json['receiveTimeStamp'] // 1000
		print(f'  Network time: {network_time} s since the nemesis block')

	# Derived fields from network time
	timestamp = network_time
	deadline = network_time + 2 * 60 * 60

	# Build the transaction [>step-1]
	root_namespace_name = os.getenv('ROOT_NAMESPACE', 'ns_root')
	child_namespace_name = os.getenv(
		'SUBNAMESPACE', f'sub_{int(time.time())}')
	full_namespace_name = (
		f'{root_namespace_name}.{child_namespace_name}')
	print(f'Creating subnamespace: {full_namespace_name}')

	rental_fee = calculate_namespace_rental_fee(False)
	print(f'  Namespace lease fee: {rental_fee / 1_000_000} XEM')

	transaction = facade.transaction_factory.create({
		'type': 'namespace_registration_transaction_v1',
		'signer_public_key': signer_key_pair.public_key,
		'timestamp': timestamp,
		'deadline': deadline,
		'rental_fee_sink': 'TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35',
		'rental_fee': rental_fee,
		'parent_name': root_namespace_name,
		'name': child_namespace_name
	})

	fee = calculate_transaction_fee(transaction)
	transaction.fee = Amount(fee)
	print(f'  Transaction fee: {fee / 1_000_000} XEM')
	# [<step-1]
	# Sign transaction and generate final payload
	signature = facade.sign_transaction(signer_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	print('Built transaction:')
	print(json.dumps(transaction.to_json(), indent=2))

	# Announce the transaction
	announce_path = '/transaction/announce'
	print(f'Announcing namespace registration to {announce_path}')
	announce_request = urllib.request.Request(
		f'{NODE_URL}{announce_path}',
		data=json_payload.encode(),
		headers={'Content-Type': 'application/json'},
		method='POST'
	)
	with urllib.request.urlopen(announce_request) as response:
		announce_result = json.loads(response.read().decode())
	print(f'  Result: {announce_result['message']}')

	# Wait for confirmation
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

	# Retrieve the namespace [>step-2]
	namespace_path = f'/namespace?namespace={full_namespace_name}'
	print(f'Fetching namespace information from {namespace_path}')
	with urllib.request.urlopen(
		f'{NODE_URL}{namespace_path}'
	) as response:
		namespace_info = json.loads(response.read().decode())
		print('Namespace information:')
		print(f'  Name: {namespace_info["fqn"]}')
		print(f'  Owner: {namespace_info["owner"]}')
		print(f'  Registration height: {namespace_info["height"]}')
	# [<step-2]
except urllib.error.URLError as e:
	print(e.reason)

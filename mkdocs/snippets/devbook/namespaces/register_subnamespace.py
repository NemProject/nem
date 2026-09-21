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


SIGNER_PRIVATE_KEY = os.getenv(
	'SIGNER_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000000')
signer_key_pair = NemFacade.KeyPair(PrivateKey(SIGNER_PRIVATE_KEY))

facade = NemFacade('testnet')
signer_address = facade.network.public_key_to_address(
	signer_key_pair.public_key)
print(f'Signer address: {signer_address}')

try:
	# Choose the subnamespace name [>step-1]
	root_namespace_name = os.getenv('ROOT_NAMESPACE', 'ns_root')
	child_namespace_name = os.getenv(
		'SUBNAMESPACE', f'sub_{int(time.time())}')
	full_namespace_name = (
		f'{root_namespace_name}.{child_namespace_name}')
	print(f'Creating subnamespace: {full_namespace_name}')
	# [<step-1]
	# Build the transaction [>step-2]
	rental_fee = calculate_namespace_rental_fee(False)
	print(f'  Namespace lease fee: {rental_fee / 1_000_000} XEM')

	transaction = facade.create_transaction_from_descriptor({
		'type': 'namespace_registration_transaction_v1',
		'rental_fee_sink': 'TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35',
		'rental_fee': rental_fee,
		'name': child_namespace_name,
		'parent_name': root_namespace_name
	}, signer_key_pair.public_key, 0, 2 * 60 * 60)

	fee = calculate_transaction_fee(transaction)
	transaction.fee = Amount(fee)
	print(f'  Transaction fee: {fee / 1_000_000} XEM')
	# [<step-2]
	# Sign transaction and generate final payload
	signature = facade.sign_transaction(signer_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	print('Built transaction:')
	print(json.dumps(transaction.to_json(), indent=2))
	transaction_hash = facade.hash_transaction(transaction)
	print(f'Transaction hash: {transaction_hash}')

	# Announce the transaction
	announce_result = announce_transaction(
		json_payload, 'namespace registration')

	# Wait for confirmation
	if 'SUCCESS' == announce_result:
		wait_for_confirmation(transaction_hash, 'namespace registration')
	else:
		print(f'Transaction rejected: {announce_result}')
	# Retrieve the namespace [>step-3]
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
	# [<step-3]
except Exception as error:
	print(error)

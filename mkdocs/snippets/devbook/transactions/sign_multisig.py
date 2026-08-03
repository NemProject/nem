import json
import os
import time
import urllib.request

from symbolchain.CryptoTypes import PrivateKey, PublicKey
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


facade = NemFacade('testnet')
# [>step-1]
MULTISIG_PUBLIC_KEY = os.getenv(
	'MULTISIG_PUBLIC_KEY',
	'D656155B48D4E71E4C59EC6FAEB5EB4F214DE8BC3C65D5BF6A3D9931B4E5ACF2')
multisig_public_key = PublicKey(MULTISIG_PUBLIC_KEY)
multisig_address = facade.network.public_key_to_address(
	multisig_public_key)
print(f'Multisig public key: {multisig_public_key}')
COSIGNATORY0_PRIVATE_KEY = os.getenv(
	'COSIGNATORY0_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000002')
cosignatory0_key_pair = NemFacade.KeyPair(
	PrivateKey(COSIGNATORY0_PRIVATE_KEY))
print(f'Cosignatory 0 public key: {cosignatory0_key_pair.public_key}')
COSIGNATORY1_PRIVATE_KEY = os.getenv(
	'COSIGNATORY1_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000003')
cosignatory1_key_pair = NemFacade.KeyPair(
	PrivateKey(COSIGNATORY1_PRIVATE_KEY))
print(f'Cosignatory 1 public key: {cosignatory1_key_pair.public_key}')
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
	# Build the inner transfer transaction [>step-3]
	transfer_transaction = facade.transaction_factory.create({
		'type': 'transfer_transaction_v2',
		'signer_public_key': multisig_public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
		'recipient_address': multisig_address,
		'amount': 1_000_000  # 1 XEM
	})
	transfer_transaction.fee = Amount(
		calculate_transaction_fee(transfer_transaction))
	# [<step-3]
	# Build the wrapper multisig transaction [>step-4]
	transaction = facade.transaction_factory.create({
		'type': 'multisig_transaction_v1',
		# This is the cosignatory that initiates the transfer
		'signer_public_key': cosignatory0_key_pair.public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
		'inner_transaction':
			facade.transaction_factory.to_non_verifiable_transaction(
				transfer_transaction)
	})
	transaction.fee = Amount(calculate_transaction_fee(transaction))
	# [<step-4]
	# Sign and announce the multisig transaction [>step-5]
	signature = facade.sign_transaction(
		cosignatory0_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	print('Built multisig transaction:')
	print(json.dumps(transaction.to_json(), indent=2))
	announce_result = announce_transaction(
		json_payload, 'multisig transaction')
	# The transaction is now waiting for the second signature
	# [<step-5]
	# Retrieve the pending transaction from the network [>step-6]
	if 'SUCCESS' == announce_result:
		cosignatory1_address = facade.network.public_key_to_address(
			cosignatory1_key_pair.public_key)
		unconfirmed_path = ('/account/unconfirmedTransactions'
			f'?address={cosignatory1_address}')
		print(f'Fetching pending transactions from {unconfirmed_path}')
		with urllib.request.urlopen(
			f'{NODE_URL}{unconfirmed_path}'
		) as response:
			pending = json.loads(response.read().decode())['data']
		# Select the pending transaction issued by the multisig account
		inner_transaction_hash = next(
			entry['meta']['data'] for entry in pending
			if entry['transaction'].get('otherTrans', {}).get(
				'signer', '').upper() == str(multisig_public_key))
		print(f'  Inner transaction hash: {inner_transaction_hash}')
		# [<step-6]
		# Build the cosignature [>step-7]
		cosignature = facade.transaction_factory.create({
			'type': 'cosignature_v1',
			# This is the cosignatory providing the second signature
			'signer_public_key': cosignatory1_key_pair.public_key,
			'timestamp': timestamp.timestamp,
			'deadline': deadline.timestamp,
			# Hash of the inner transfer transaction
			'other_transaction_hash': inner_transaction_hash,
			# Address of the multisig account
			'multisig_account_address': multisig_address
		})
		cosignature.fee = Amount(calculate_transaction_fee(cosignature))
		# [<step-7]
		# Sign and announce the cosignature [>step-8]
		cosignature_signature = facade.sign_transaction(
			cosignatory1_key_pair, cosignature)
		cosignature_payload = facade.transaction_factory.attach_signature(
			cosignature, cosignature_signature)
		print('Built cosignature:')
		print(json.dumps(cosignature.to_json(), indent=2))
		cosignature_result = announce_transaction(
			cosignature_payload, 'cosignature')
		# [<step-8]
		# Wait for the multisig transaction to be confirmed [>step-9]
		if 'SUCCESS' == cosignature_result:
			wait_for_confirmation(
				facade.hash_transaction(transaction),
				'multisig transaction')
		else:
			print(f'Transaction rejected: {cosignature_result}')
		# [<step-9]
	else:
		print(f'Transaction rejected: {announce_result}')
except urllib.error.URLError as e:
	print(e.reason)

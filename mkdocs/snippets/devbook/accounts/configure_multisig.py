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

facade = NemFacade('testnet')
# [>step-1]
KEY_TEMPLATE = '0' * 63 + '{}'

# Set up the keys for the multisig account and its two cosignatories
MULTISIG_PRIVATE_KEY = os.getenv(
	'MULTISIG_PRIVATE_KEY', KEY_TEMPLATE.format(1))
multisig_key_pair = NemFacade.KeyPair(PrivateKey(MULTISIG_PRIVATE_KEY))
multisig_address = facade.network.public_key_to_address(
	multisig_key_pair.public_key)
print(f'Multisig address: {multisig_address} '
	f'(public key {multisig_key_pair.public_key})')

cosignatory_key_pairs = []
for i in range(2):
	COSIGNATORY_PRIVATE_KEY = os.getenv(
		f'COSIGNATORY{i}_PRIVATE_KEY', KEY_TEMPLATE.format(i + 2))
	key_pair = NemFacade.KeyPair(PrivateKey(COSIGNATORY_PRIVATE_KEY))
	cosignatory_key_pairs.append(key_pair)
	addr = facade.network.public_key_to_address(key_pair.public_key)
	print(f'Cosignatory {i} address: '
		f'{addr} (public key {key_pair.public_key})')  # [<step-1]


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


# Returns the cosignatory addresses of the provided multisig [>step-3]
# account, or an empty list if the account is not multisig
def get_multisig_cosignatories(address):
	account_path = f'/account/get?address={address}'
	print(f'Getting cosignatories from {account_path}')
	url = f'{NODE_URL}{account_path}'
	with urllib.request.urlopen(url) as account_response:
		account_info = json.loads(account_response.read().decode())
		found_cosignatories = [
			cosignatory['address']
			for cosignatory in account_info['meta']['cosignatories']
		]
		if not found_cosignatories:
			print('  Response: No cosignatories')
			return []
		print(f'  Response: {found_cosignatories}')
		return found_cosignatories  # [<step-3]


# [>step-5]
# Returns a transaction that turns a regular account into a multisig
def multisig_enable_transaction(tx_timestamp, tx_deadline,
		approval_delta):
	# Create a multisig account modification transaction
	# that adds the cosignatories
	modifications = [
		{'modification': {
			'modification_type': 'add_cosignatory',
			'cosignatory_public_key': key_pair.public_key
		}}
		for key_pair in cosignatory_key_pairs
	]
	transaction = facade.transaction_factory.create({
		'type': 'multisig_account_modification_transaction_v2',
		# This is the account that will be turned into a multisig
		'signer_public_key': multisig_key_pair.public_key,
		'timestamp': tx_timestamp.timestamp,
		'deadline': tx_deadline.timestamp,
		# Change of the number of cosignatures
		# required to approve transactions
		'min_approval_delta': approval_delta,
		'modifications': modifications
	})
	# [<step-5]
	# Calculate and attach the transaction fee [>step-6]
	fee = calculate_transaction_fee(transaction)
	transaction.fee = Amount(fee)
	print(f'  Transaction fee: {fee / 1_000_000} XEM')
	print('Enabling the multisig with the modification transaction:')
	print(json.dumps(transaction.to_json(), indent=2))
	# [<step-6]
	# Sign the transaction with the multisig's key [>step-7]
	signature = facade.sign_transaction(multisig_key_pair, transaction)
	facade.transaction_factory.attach_signature(transaction, signature)
	return transaction  # [<step-7]


# [>step-8]
# Returns a transaction that removes one cosignatory from the multisig
def multisig_removal_transaction(tx_timestamp, tx_deadline,
		removed_key_pair, approval_delta):
	# Create a multisig account modification transaction
	# that removes a single cosignatory
	inner_transaction = facade.transaction_factory.create({
		'type': 'multisig_account_modification_transaction_v2',
		# This is the multisig account that will be modified
		'signer_public_key': multisig_key_pair.public_key,
		'timestamp': tx_timestamp.timestamp,
		'deadline': tx_deadline.timestamp,
		# Change of the number of cosignatures
		# required to approve transactions
		'min_approval_delta': approval_delta,
		'modifications': [
			{'modification': {
				'modification_type': 'delete_cosignatory',
				'cosignatory_public_key': removed_key_pair.public_key
			}}
		]
	})
	# [<step-8]
	# Wrap the modification in a multisig transaction [>step-9]
	inner_fee = calculate_transaction_fee(inner_transaction)
	inner_transaction.fee = Amount(inner_fee)
	transaction = facade.transaction_factory.create({
		'type': 'multisig_transaction_v1',
		# This is the cosignatory that initiates the removal
		'signer_public_key': cosignatory_key_pairs[0].public_key,
		'timestamp': tx_timestamp.timestamp,
		'deadline': tx_deadline.timestamp,
		'inner_transaction':
			facade.transaction_factory.to_non_verifiable_transaction(
				inner_transaction)
	})
	# [<step-9]
	# Calculate and attach the transaction fee [>step-10]
	fee = calculate_transaction_fee(transaction)
	transaction.fee = Amount(fee)
	print(f'  Transaction fee: {(inner_fee + fee) / 1_000_000} XEM')
	print('Disabling the multisig with the multisig transaction:')
	print(json.dumps(transaction.to_json(), indent=2))
	# [<step-10]
	# Sign the transaction with the cosignatory's key [>step-11]
	signature = facade.sign_transaction(
		cosignatory_key_pairs[0], transaction)
	facade.transaction_factory.attach_signature(transaction, signature)
	return transaction  # [<step-11]


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
	# Get current state of the multisig account and decide which [>step-4]
	# operation to perform
	cosignatories = get_multisig_cosignatories(multisig_address)
	if len(cosignatories) == 0:
		# Enable the multisig
		transactions = [multisig_enable_transaction(
			timestamp, deadline, 1)]
	else:
		# Disable the multisig
		transactions = [
			multisig_removal_transaction(
				timestamp, deadline, cosignatory_key_pairs[1], 0),
			multisig_removal_transaction(
				timestamp, deadline, cosignatory_key_pairs[0], -1)
		]
	# [<step-4]
	# Announce each transaction and wait for confirmation [>step-12]
	for signed_transaction in transactions:
		transaction_hash = facade.hash_transaction(signed_transaction)
		print(f'Built transaction with hash: {transaction_hash}')
		json_payload = facade.transaction_factory.to_json(
			signed_transaction)
		announce_result = announce_transaction(
			json_payload, 'transaction')
		if 'SUCCESS' != announce_result:
			print('Transaction rejected')
			break
		wait_for_confirmation(transaction_hash, 'transaction')
	# [<step-12]
except Exception as e:
	print(e)

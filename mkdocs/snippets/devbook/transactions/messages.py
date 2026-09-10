import json
import os
import time
import urllib.error
import urllib.request
from binascii import hexlify

from symbolchain.CryptoTypes import PrivateKey, PublicKey
from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nc import Amount, Message, MessageType
from symbolchain.nem.FeeCalculator import calculate_transaction_fee
from symbolchain.nem.MessageEncoder import MessageEncoder
from symbolchain.nem.Network import NetworkTimestamp

# Configuration
NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')


# Helper function to poll for confirmed transaction
def retrieve_confirmed_transaction(hash_value, label):
	print(f'Polling for {label} confirmation...')
	attempts = 0
	max_attempts = 120

	while attempts < max_attempts:
		try:
			url = f'{NODE_URL}/transaction/get?hash={hash_value}'
			with urllib.request.urlopen(url) as transaction_confirmed:
				print(f'  {label} confirmed!')
				return json.loads(transaction_confirmed.read().decode())
		except urllib.error.HTTPError:
			# Transaction not yet confirmed
			pass
		attempts += 1
		time.sleep(2)

	raise TimeoutError(
		f'{label} not confirmed after {max_attempts} attempts'
	)


# Set up sender and recipient accounts [>step-1]
facade = NemFacade('testnet')

sender_private_key_string = os.getenv(
	'SENDER_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000000',
)
sender_key_pair = NemFacade.KeyPair(
	PrivateKey(sender_private_key_string)
)
sender_address = facade.network.public_key_to_address(
	sender_key_pair.public_key
)

recipient_private_key_string = os.getenv(
	'RECIPIENT_PRIVATE_KEY',
	'1111111111111111111111111111111111111111111111111111111111111111',
)
recipient_key_pair = NemFacade.KeyPair(
	PrivateKey(recipient_private_key_string)
)
recipient_address = facade.network.public_key_to_address(
	recipient_key_pair.public_key
)

print(f'Sender address: {sender_address}')
print(f'Recipient address: {recipient_address}\n')
# [<step-1]
# Fetch current network time
time_path = '/time-sync/network-time'
print(f'Fetching current network time from {time_path}')
with urllib.request.urlopen(f'{NODE_URL}{time_path}') as response:
	response_json = json.loads(response.read().decode())
	network_time = response_json['receiveTimeStamp'] // 1000
	timestamp = NetworkTimestamp(network_time)
	deadline = timestamp.add_hours(2)
	print(f'  Network time: {network_time} s since the nemesis block\n')

# ===== PLAIN TEXT MESSAGE =====
print('==> Sending Plain Text Message')  # [>step-2]

# Create a plain text message
plain_message = 'Hello, NEM!'.encode('utf-8')
print(f'Plain message: {plain_message.decode("utf-8")}')

# Build transfer transaction with plain message
plain_transaction = facade.transaction_factory.create(
	{
		'type': 'transfer_transaction_v2',
		'signer_public_key': sender_key_pair.public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
		'recipient_address': recipient_address,
		'amount': 0,
		'message': {
			'message_type': 'plain',
			'message': plain_message,
		},
	}
)  # [<step-2]
plain_transaction.fee = Amount(
	calculate_transaction_fee(plain_transaction))

# Sign and announce the transaction
plain_signature = facade.sign_transaction(
	sender_key_pair, plain_transaction
)
plain_json_payload = facade.transaction_factory.attach_signature(
	plain_transaction, plain_signature
)
plain_transaction_hash = facade.hash_transaction(
	plain_transaction
)
print(f'Transaction hash: {plain_transaction_hash}')

plain_announce_request = urllib.request.Request(
	f'{NODE_URL}/transaction/announce',
	data=plain_json_payload.encode('utf-8'),
	headers={'Content-Type': 'application/json'},
	method='POST',
)
with urllib.request.urlopen(plain_announce_request) as response:
	print('Plain message transaction announced\n')

# ===== RECEIVING PLAIN TEXT MESSAGE =====
print('<== Receiving Plain Text Message')  # [>step-3]

# Wait for confirmation
plain_tx_data = retrieve_confirmed_transaction(
	plain_transaction_hash, 'Plain message transaction'
)

# Decode plain message from confirmed transaction
received_plain_message = bytes.fromhex(
	plain_tx_data['transaction']['message']['payload']
)
print(
	f'Received plain message: {received_plain_message.decode("utf-8")}\n'
)
# [<step-3]
# ===== ENCRYPTED MESSAGE =====
print('==> Sending Encrypted Message')  # [>step-4]

# Create a message encoder with sender's key pair
sender_message_encoder = MessageEncoder(sender_key_pair)

# Encrypt the message using recipient's public key
secret_message = 'This is a secret message!'.encode('utf-8')
encrypted_message = sender_message_encoder.encode(
	recipient_key_pair.public_key, secret_message
)
print(f'Original message: {secret_message.decode("utf-8")}')
encrypted_payload = hexlify(encrypted_message.message).decode('utf-8')
print(f'Encrypted payload: {encrypted_payload}')

# Build transfer transaction with encrypted message
encrypted_transaction = facade.transaction_factory.create(
	{
		'type': 'transfer_transaction_v2',
		'signer_public_key': sender_key_pair.public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
		'recipient_address': recipient_address,
		'amount': 0,
		'message': {
			'message_type': 'encrypted',
			'message': encrypted_message.message,
		},
	}
)  # [<step-4]
encrypted_transaction.fee = Amount(
	calculate_transaction_fee(encrypted_transaction))

# Sign and announce the transaction
encrypted_signature = facade.sign_transaction(
	sender_key_pair, encrypted_transaction
)
encrypted_json_payload = facade.transaction_factory.attach_signature(
	encrypted_transaction, encrypted_signature
)
encrypted_transaction_hash = facade.hash_transaction(
	encrypted_transaction
)
print(f'Transaction hash: {encrypted_transaction_hash}')

encrypted_announce_request = urllib.request.Request(
	f'{NODE_URL}/transaction/announce',
	data=encrypted_json_payload.encode('utf-8'),
	headers={'Content-Type': 'application/json'},
	method='POST',
)
with urllib.request.urlopen(encrypted_announce_request) as response:
	print('Encrypted message transaction announced\n')

# ===== RECEIVING ENCRYPTED MESSAGE =====
print('<== Receiving Encrypted Message')  # [>step-5]

# Wait for confirmation
encrypted_tx_data = retrieve_confirmed_transaction(
	encrypted_transaction_hash, 'Encrypted message transaction'
)

# Decode encrypted message using recipient's private key
recipient_message_encoder = MessageEncoder(recipient_key_pair)
received_encrypted_message = Message()
received_encrypted_message.message_type = MessageType.ENCRYPTED
received_encrypted_message.message = bytes.fromhex(
	encrypted_tx_data['transaction']['message']['payload']
)

# Get sender's public key from the transaction
sender_public_key_from_tx = PublicKey(
	encrypted_tx_data['transaction']['signer']
)

(is_decoded, decrypted_message) = recipient_message_encoder.try_decode(
	sender_public_key_from_tx, received_encrypted_message
)

if is_decoded:
	message_text = decrypted_message.decode('utf-8')
	print(f'Recipient decrypted message: {message_text}')
else:
	print('Recipient failed to decrypt message')  # [<step-5]

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

# Configuration
NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')


# Helper function to wait for transaction confirmation
def wait_for_confirmation(tx_hash, label):
	status_path = f'/transaction/get?hash={tx_hash}'
	print(f'Waiting for {label} confirmation from {status_path}')
	is_confirmed = False
	confirmed = None
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
	return confirmed


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
# --- PLAIN TEXT MESSAGE ---
print('==> Sending Plain Text Message')  # [>step-2]

# Create a plain text message
plain_message = 'Hello, NEM!'.encode('utf-8')
print(f'Plain message: {plain_message.decode("utf-8")}')

# Build transfer transaction with plain message
plain_transaction = facade.create_transaction_from_descriptor(
	{
		'type': 'transfer_transaction_v2',
		'recipient_address': recipient_address,
		'amount': 0,
		'message': {
			'message_type': 'plain',
			'message': plain_message,
		},
	},
	sender_key_pair.public_key,
	0,
	2 * 60 * 60
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

# --- RECEIVING PLAIN TEXT MESSAGE ---
print('<== Receiving Plain Text Message')  # [>step-3]

# Wait for confirmation
plain_tx_data = wait_for_confirmation(
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
# --- ENCRYPTED MESSAGE ---
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
encrypted_transaction = facade.create_transaction_from_descriptor(
	{
		'type': 'transfer_transaction_v2',
		'recipient_address': recipient_address,
		'amount': 0,
		'message': {
			'message_type': 'encrypted',
			'message': encrypted_message.message,
		},
	},
	sender_key_pair.public_key,
	0,
	2 * 60 * 60
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

# --- RECEIVING ENCRYPTED MESSAGE ---
print('<== Receiving Encrypted Message')  # [>step-5]

# Wait for confirmation
encrypted_tx_data = wait_for_confirmation(
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

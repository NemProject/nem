import asyncio
import json
import os
import random
import urllib.request
import uuid

import stomper
from symbolchain.CryptoTypes import PrivateKey, PublicKey
from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nc import Amount
from symbolchain.nem.FeeCalculator import calculate_transaction_fee
from symbolchain.nem.Network import NetworkTimestamp
from websockets import connect

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
WS_URL = NODE_URL.replace(':7890', ':7778')
print(f'Using node {NODE_URL}')


# SockJS has no Python client library.
# These helpers wrap the raw WebSocket transport to mirror a STOMP client.
def sockjs_url(endpoint_url):
	# SockJS raw WebSocket transport adds a random server and session id
	server = random.randint(100, 999)
	session = uuid.uuid4().hex
	ws_base = endpoint_url.replace('http', 'ws', 1)
	return f'{ws_base}/{server}/{session}/websocket'


async def send_frame(websocket, frame):
	# SockJS wraps each client payload as a JSON array of frame strings
	await websocket.send(json.dumps([frame]))


async def stomp_connect(websocket):
	await websocket.recv()  # consume the SockJS open frame
	await send_frame(
		websocket, stomper.connect('', '', NODE_URL, heartbeats=(0, 0)))


async def stomp_subscribe(websocket, destination, sub_id):
	await send_frame(websocket, stomper.subscribe(destination, sub_id))


async def stomp_send(websocket, destination, body):
	await send_frame(websocket, stomper.send(destination, body))


async def stomp_unsubscribe(websocket, sub_id):
	await send_frame(websocket, stomper.unsubscribe(sub_id))


async def stomp_disconnect(websocket):
	await send_frame(websocket, stomper.disconnect())


def stomp_messages(raw_frame):
	# Yield each STOMP MESSAGE frame in a SockJS data frame
	if 'a' != raw_frame[0]:  # skip 'o' open, 'h' heartbeat, 'c' close
		return
	for payload in json.loads(raw_frame[1:]):
		frame = stomper.unpack_frame(payload)
		if 'MESSAGE' == frame['cmd']:
			yield frame


async def stomp_frames(websocket):
	# Yield each STOMP MESSAGE frame as it arrives
	async for raw_frame in websocket:
		for frame in stomp_messages(raw_frame):
			yield frame


facade = NemFacade('testnet')
# Set up the multisig and cosignatory accounts [>step-1]
MULTISIG_PUBLIC_KEY = os.getenv(
	'MULTISIG_PUBLIC_KEY',
	'D656155B48D4E71E4C59EC6FAEB5EB4F214DE8BC3C65D5BF6A3D9931B4E5ACF2')
multisig_public_key = PublicKey(MULTISIG_PUBLIC_KEY)
multisig_address = str(facade.network.public_key_to_address(
	multisig_public_key))
print(f'Multisig address: {multisig_address}')
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


async def main():
	# [Cosignatory 0] Build and sign the multisig transaction [>step-2]
	with urllib.request.urlopen(
		f'{NODE_URL}/time-sync/network-time'
	) as resp:
		network_time = json.loads(
			resp.read().decode())['receiveTimeStamp'] // 1000
	timestamp = NetworkTimestamp(network_time)
	deadline = timestamp.add_hours(2)

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

	transaction = facade.transaction_factory.create({
		'type': 'multisig_transaction_v1',
		'signer_public_key': cosignatory0_key_pair.public_key,
		'timestamp': timestamp.timestamp,
		'deadline': deadline.timestamp,
		'inner_transaction':
			facade.transaction_factory.to_non_verifiable_transaction(
				transfer_transaction)
	})
	transaction.fee = Amount(calculate_transaction_fee(transaction))

	signature = facade.sign_transaction(
		cosignatory0_key_pair, transaction)
	json_payload = facade.transaction_factory.attach_signature(
		transaction, signature)
	transaction_hash = str(facade.hash_transaction(transaction)).upper()
	print('[Cosignatory 0] Built multisig transaction '
		f'{transaction_hash[:16]}...')
	# [<step-2]
	# [Cosignatory 1] Connect to the WebSocket [>step-3]
	endpoint = f'{WS_URL}/w/messages'
	async with connect(sockjs_url(endpoint)) as websocket:
		await stomp_connect(websocket)
		print(f'[Cosignatory 1] Connected to {WS_URL}')
		frames = stomp_frames(websocket)
		# [<step-3]
		# [Cosignatory 1] Subscribe to the multisig account channels [>step-4]
		account_channel = f'/account/{multisig_address}'
		channels = {
			account_channel: 'id-0',
			f'/unconfirmed/{multisig_address}': 'id-1',
			f'/transactions/{multisig_address}': 'id-2',
		}
		for channel, sub_id in channels.items():
			await stomp_subscribe(websocket, channel, sub_id)
			print(f'[Cosignatory 1] Subscribed to {channel} channel')
		# [<step-4]
		# [Cosignatory 1] Register the multisig account [>step-5]
		await stomp_send(websocket, '/w/api/account/get',
			json.dumps({'account': multisig_address}))
		async for frame in frames:
			if account_channel == frame['headers']['destination']:
				balance = json.loads(
					frame['body'])['account']['balance']
				print(f'Account update: balance={balance}')
				break
		print('[Cosignatory 1] Multisig account registered')
		# [<step-5]
		# [Cosignatory 0] Announce the multisig transaction [>step-6]
		print('[Cosignatory 0] Announcing multisig transaction '
			f'{transaction_hash[:16]}...')
		announce_request = urllib.request.Request(
			f'{NODE_URL}/transaction/announce',
			data=json_payload.encode(),
			headers={'Content-Type': 'application/json'},
			method='POST'
		)
		with urllib.request.urlopen(announce_request) as resp:
			result = json.loads(resp.read().decode())
		if 'SUCCESS' != result['message']:
			print(f'Transaction rejected: {result["message"]}')
			return
		# The transaction is now waiting for the second signature
		# [<step-6]
		# [Cosignatory 1] Select the pending multisig transaction [>step-7]
		inner_transaction_hash = None
		async for frame in frames:
			destination = frame['headers']['destination']
			body = json.loads(frame['body'])
			if '/unconfirmed/' not in destination:
				continue
			signer = body['transaction'].get(
				'otherTrans', {}).get('signer', '')
			if signer.upper() != str(multisig_public_key):
				continue
			inner_transaction_hash = body['meta']['innerHash']['data']
			print('unconfirmed: innerHash='
				f'{inner_transaction_hash[:16]}...')
			# [<step-7]
			# [Cosignatory 1] Cosign the pending transaction [>step-8]
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
			cosignature.fee = Amount(
				calculate_transaction_fee(cosignature))
			cosignature_signature = facade.sign_transaction(
				cosignatory1_key_pair, cosignature)
			cosignature_payload = (
				facade.transaction_factory.attach_signature(
					cosignature, cosignature_signature))
			cosignature_request = urllib.request.Request(
				f'{NODE_URL}/transaction/announce',
				data=cosignature_payload.encode(),
				headers={'Content-Type': 'application/json'},
				method='POST'
			)
			with urllib.request.urlopen(cosignature_request) as resp:
				cosignature_result = json.loads(resp.read().decode())
			if 'SUCCESS' != cosignature_result['message']:
				print('Cosignature rejected: '
					f'{cosignature_result["message"]}')
				return
			print('[Cosignatory 1] Announced cosignature')
			break
		# [<step-8]
		# [Cosignatory 1] Wait for confirmation [>step-9]
		confirmed = False
		async for frame in frames:
			destination = frame['headers']['destination']
			body = json.loads(frame['body'])
			if account_channel == destination:
				balance = body['account']['balance']
				print(f'Account update: balance={balance}')
				if confirmed:
					break
			elif '/transactions/' in destination:
				message_hash = body['meta']['innerHash']['data']
				print(f'confirmed: innerHash={message_hash[:16]}...')
				matched = message_hash == inner_transaction_hash
				if matched and not confirmed:
					print('Multisig transaction confirmed')
					confirmed = True
		# [<step-9]
		# [Cosignatory 1] Unsubscribe before closing [>step-10]
		for sub_id in channels.values():
			await stomp_unsubscribe(websocket, sub_id)
		print('[Cosignatory 1] Unsubscribed from all channels')
		await stomp_disconnect(websocket)  # [<step-10]


try:
	asyncio.run(main())
except Exception as error:
	print(error)

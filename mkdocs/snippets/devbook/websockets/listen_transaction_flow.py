import asyncio
import json
import os
import random
import urllib.request
import uuid

import stomper
from symbolchain.CryptoTypes import PrivateKey
from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nc import Amount
from symbolchain.nem.FeeCalculator import calculate_transaction_fee
from websockets import connect

NODE_HOST = os.getenv('NODE_HOST', 'libertalia.nemtest.net')
NODE_URL = f'http://{NODE_HOST}:7890'
WS_URL = f'http://{NODE_HOST}:7778'
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


def stomp_messages(raw):
	# Yield each STOMP MESSAGE frame in a SockJS data frame
	if 'a' != raw[0]:  # skip 'o' open, 'h' heartbeat, 'c' close
		return
	for payload in json.loads(raw[1:]):
		frame = stomper.unpack_frame(payload)
		if 'MESSAGE' == frame['cmd']:
			yield frame


# Set up the monitored address and signer [>step-1]
MONITOR_ADDRESS = os.getenv(
	'MONITOR_ADDRESS',
	'TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4'
)
print(f'Monitoring address: {MONITOR_ADDRESS}')

SIGNER_PRIVATE_KEY = os.getenv(
	'SIGNER_PRIVATE_KEY',
	'0000000000000000000000000000000000000000000000000000000000000000'
)
facade = NemFacade('testnet')
signer_key_pair = NemFacade.KeyPair(PrivateKey(SIGNER_PRIVATE_KEY))  # [<step-1]


async def main():
	# Connect to the WebSocket [>step-2]
	endpoint = f'{WS_URL}/w/messages'
	async with connect(sockjs_url(endpoint)) as websocket:
		await stomp_connect(websocket)
		print(f'Connected to {WS_URL}')
		# [<step-2]
		# Register the account and confirm it is active [>step-3]
		account = f'/account/{MONITOR_ADDRESS}'
		await stomp_subscribe(websocket, account, 'id-0')
		await stomp_send(websocket, '/w/api/account/get',
			json.dumps({'account': MONITOR_ADDRESS}))
		async for raw in websocket:
			if any(f['headers']['destination'] == account
					for f in stomp_messages(raw)):
				break
		await stomp_unsubscribe(websocket, 'id-0')
		print('Account registered')
		# [<step-3]
		# Subscribe to the transaction channels [>step-4]
		channels = {
			f'/unconfirmed/{MONITOR_ADDRESS}': 'id-1',
			f'/transactions/{MONITOR_ADDRESS}': 'id-2',
		}
		for destination, sub_id in channels.items():
			await stomp_subscribe(websocket, destination, sub_id)
			print(f'Subscribed to {destination} channel')
		# [<step-4]
		# Build and sign a transfer to the monitored address [>step-5]
		with urllib.request.urlopen(
			f'{NODE_URL}/time-sync/network-time'
		) as resp:
			network_time = json.loads(
				resp.read().decode())['receiveTimeStamp'] // 1000
		transaction = facade.transaction_factory.create({
			'type': 'transfer_transaction_v2',
			'signer_public_key': signer_key_pair.public_key,
			'timestamp': network_time,
			'deadline': network_time + 2 * 60 * 60,
			'recipient_address': MONITOR_ADDRESS,
			'amount': 0,
		})
		transaction.fee = Amount(calculate_transaction_fee(transaction))
		signature = facade.sign_transaction(signer_key_pair, transaction)
		json_payload = facade.transaction_factory.attach_signature(
			transaction, signature)
		transaction_hash = str(facade.hash_transaction(transaction))
		# [<step-5]
		# Announce the transaction and wait for it to confirm [>step-6]
		print(f'Announcing transaction {transaction_hash[:16]}...')
		announce_request = urllib.request.Request(
			f'{NODE_URL}/transaction/announce',
			data=json_payload.encode(),
			headers={'Content-Type': 'application/json'},
			method='POST'
		)
		with urllib.request.urlopen(announce_request) as resp:
			result = json.loads(resp.read().decode())

		if 'SUCCESS' == result['message']:
			expected_hash = transaction_hash.upper()
			confirmed = False
			async for raw in websocket:
				for frame in stomp_messages(raw):
					destination = frame['headers']['destination']
					pair = json.loads(frame['body'])
					message_hash = pair['meta']['hash']['data']
					name = ('confirmed' if '/transactions/' in destination
							else 'unconfirmed')
					print(f'{name}: hash={message_hash[:16]}...')
					is_match = message_hash.upper() == expected_hash
					if name == 'confirmed' and is_match:
						short = transaction_hash[:16]
						print(f'Transaction {short}... confirmed')
						confirmed = True
				if confirmed:
					break
		else:
			print(f'Transaction rejected: {result["message"]}')
		# [<step-6]
		# Unsubscribe before closing [>step-7]
		for sub_id in channels.values():
			await stomp_unsubscribe(websocket, sub_id)
		print('Unsubscribed from all channels')
		await stomp_disconnect(websocket)  # [<step-7]


try:
	asyncio.run(main())
except Exception as error:
	print(error)

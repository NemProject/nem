import asyncio
import json
import os
import random
import uuid

import stomper
from websockets import connect

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7778')
print(f'Using node {NODE_URL}')


# SockJS has no Python client library.
# These helpers wrap the raw WebSocket transport.
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


async def stomp_unsubscribe(websocket, sub_id):
	await send_frame(websocket, stomper.unsubscribe(sub_id))


async def stomp_disconnect(websocket):
	await send_frame(websocket, stomper.disconnect())


def stomp_messages(raw):
	# Yield the JSON body of each STOMP MESSAGE in a SockJS data frame
	if 'a' != raw[0]:  # skip 'o' open, 'h' heartbeat, 'c' close
		return
	for payload in json.loads(raw[1:]):
		frame = stomper.unpack_frame(payload)
		if 'MESSAGE' == frame['cmd']:
			yield json.loads(frame['body'])


async def main():
	# Open connection [>step-1]
	async with connect(sockjs_url(f'{NODE_URL}/w/messages')) as websocket:
		await stomp_connect(websocket)
		print(f'Connected to {NODE_URL}')
		# [<step-1]
		# Subscribe to the new block channel [>step-2]
		await stomp_subscribe(websocket, '/blocks', 'id-0')
		print('Subscribed to /blocks channel')
		# [<step-2]
		# Read and format each new block [>step-3]
		try:
			async for raw in websocket:
				for block in stomp_messages(raw):
					print(
						f'New block: height={block["height"]:,}'
						f' harvester={block["signer"][:16]}...'
					)
		# [<step-3]
		# Unsubscribe on exit [>step-4]
		finally:
			await stomp_unsubscribe(websocket, 'id-0')
			await stomp_disconnect(websocket)
			print('Unsubscribed and disconnected')
		# [<step-4]

try:
	asyncio.run(main())
except KeyboardInterrupt:
	pass
except Exception as error:
	print(error)

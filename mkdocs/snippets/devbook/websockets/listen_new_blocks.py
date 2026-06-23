import asyncio
import json
import os
import random
import uuid

import stomper
from websockets import connect

NODE_URL = os.getenv('NODE_URL', 'http://ntn1.dusanjp.com:7778')
print(f'Using node {NODE_URL}')


# SockJS has no Python client library.
# These helpers wrap the raw WebSocket transport.
def sockjs_url(node_url):
	# SockJS raw WebSocket transport adds a random server and session id
	server = random.randint(100, 999)
	session = uuid.uuid4().hex
	ws_base = node_url.replace('http', 'ws', 1)
	return f'{ws_base}/w/messages/{server}/{session}/websocket'


def sockjs_send(frame):
	# SockJS wraps each client payload as a JSON array of frame strings
	return json.dumps([frame])


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
	async with connect(sockjs_url(NODE_URL)) as websocket:
		await websocket.recv()
		await websocket.send(sockjs_send(
			stomper.connect('', '', NODE_URL, heartbeats=(0, 0))))
		print(f'Connected to {NODE_URL}')
		# [<step-1]
		# Subscribe to the new block channel [>step-2]
		await websocket.send(sockjs_send(
			stomper.subscribe('/blocks', 'sub-blocks')))
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
			await websocket.send(
				sockjs_send(stomper.unsubscribe('sub-blocks')))
			await websocket.send(sockjs_send(stomper.disconnect()))
			print('Unsubscribed and disconnected')
		# [<step-4]

try:
	asyncio.run(main())
except KeyboardInterrupt:
	pass
except Exception as error:
	print(error)

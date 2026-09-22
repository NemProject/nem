import json
import os
import time
import urllib.request

NODE_URL = os.getenv('NODE_URL', 'http://libertalia.nemtest.net:7890')
print(f'Using node {NODE_URL}')

prev_height = None
height_changed_at = None

REWRITE_LIMIT = 360

try:
	while True:
		with urllib.request.urlopen(  # [>step-1]
			f'{NODE_URL}/chain/height'
		) as response:
			chain_height = json.loads(response.read().decode())

		height = int(chain_height['height'])  # [<step-1]
		# [>step-2]
		irreversible_height = max(0, height - REWRITE_LIMIT)  # [<step-2]
		# [>step-3]
		now = time.time()
		if prev_height is not None and height != prev_height:
			height_changed_at = now

		if height_changed_at is not None:
			height_ago = f'{int(now - height_changed_at)}s ago'
		else:
			height_ago = '-'  # [<step-3]
		# [>step-4]
		print(
			f'Height: {height:>10,}  (changed {height_ago})'
			f'  |  Irreversible: {irreversible_height:>10,}'
		)

		prev_height = height
		time.sleep(1)
		# [<step-4]
except KeyboardInterrupt:
	pass
except Exception as error:
	print(error)

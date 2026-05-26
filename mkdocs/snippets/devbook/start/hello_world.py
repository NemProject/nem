import json
import urllib.request

from symbolchain.facade.NemFacade import NemFacade
from symbolchain.nem.Network import NetworkTimestamp

# [>step-1]
facade = NemFacade('testnet')
print(f"Network name: {facade.network.name}")
# NetworkTimestamp(0) is the genesis block timestamp
launch_date = facade.network.to_datetime(NetworkTimestamp(0))
print(f"Network launch date: {launch_date}")  # [<step-1]
# [>step-2]
NODE_URL = 'http://tortuga.nemtest.net:7890'
print(f'Using node {NODE_URL}')
try:
	# Fetch current chain height
	height_path = '/chain/height'
	print(f'Fetching chain height from {height_path}')
	with urllib.request.urlopen(
		f'{NODE_URL}{height_path}', timeout=10
	) as response:
		response_json = json.loads(response.read().decode())
		height = int(response_json['height'])
		print(f"  Blockchain height: {height:,} blocks")

except urllib.error.URLError as e:
	print(e.reason)  # [<step-2]

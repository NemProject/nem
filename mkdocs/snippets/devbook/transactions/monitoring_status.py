import json
import os
import time
import urllib.error
import urllib.request

NODE_URL = os.getenv("NODE_URL", "http://libertalia.nemtest.net:7890")
print(f'Using node {NODE_URL}')

# [>step-1]
# Transaction hash to monitor
transaction_hash = os.getenv(
	"TRANSACTION_HASH",
	"AE0B2142DFB75C9C126442EF612944E926BCE63FA34B353CDE409E2E87703C0B")
# Signer's address
signer_address = os.getenv(
	"SIGNER_ADDRESS",
	"TBONKWCOWBZYZB2I5JD3LSDBQVBYHB757VN3SKPP")
# Transaction signature
transaction_signature = os.getenv(
	"TRANSACTION_SIGNATURE",
	"99B1850FADDB964112D030AA0A5C9F8B5B1B6B992B407D9C70F52F089BD651DF"
	"A7D4991639A48B810EFD98C45060D7AD9AE57FDA37F58561459DCE8D0A747F02")
# [<step-1]
print(f"Monitoring transaction: {transaction_hash}")


def wait_for_transaction_confirmation(  # [>step-2]
	tx_hash, max_attempts=120, wait_seconds=1
):
	"""
	Poll /transaction/get until the transaction is confirmed.

	Args:
		tx_hash: hash of the transaction to monitor
		max_attempts: maximum polling attempts
		wait_seconds: seconds to wait between attempts

	Returns:
		True if the transaction was confirmed, False otherwise
	"""
	status_path = f"/transaction/get?hash={tx_hash}"
	print("\nWaiting for transaction confirmation")
	print(f"Polling {status_path}")

	for attempt in range(1, max_attempts + 1):
		try:
			url = f"{NODE_URL}{status_path}"
			with urllib.request.urlopen(url) as response:
				confirmed = json.loads(response.read().decode())
				height = confirmed["meta"]["height"]
				print(f"  Attempt {attempt}: confirmed in block {height}")
				return True
		except urllib.error.HTTPError as err:
			if err.status != 400:
				raise
			print(f"  Attempt {attempt}: pending")
		# Wait before next attempt (except on last attempt)
		if attempt < max_attempts:
			time.sleep(wait_seconds)
	return False
	# [<step-2]


def is_in_unconfirmed_pool(signature, address):  # [>step-3]
	"""
	Check whether a transaction with the given signature is in the
	address's unconfirmed pool.
	"""
	path = f"/account/unconfirmedTransactions?address={address}"
	with urllib.request.urlopen(f"{NODE_URL}{path}") as response:
		pool = json.loads(response.read().decode())["data"]

	target = signature.lower()
	return any(
		entry["transaction"]["signature"].lower() == target
		for entry in pool
	)
	# [<step-3]


# [>step-4]
if wait_for_transaction_confirmation(transaction_hash):
	print("\nTransaction confirmed!")
elif is_in_unconfirmed_pool(transaction_signature, signer_address):
	print("\nTransaction still in the unconfirmed pool.")
else:
	print("\nTransaction not in the unconfirmed pool.")
# [<step-4]

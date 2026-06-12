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


def get_confirmation_height(tx_hash):  # [>step-2]
	"""
	Query /transaction/get once to check for confirmation.

	Args:
		tx_hash: hash of the transaction to check

	Returns:
		The height of the block containing the transaction, or None
		if the transaction is not confirmed yet
	"""
	url = f"{NODE_URL}/transaction/get?hash={tx_hash}"
	try:
		with urllib.request.urlopen(url) as response:
			confirmed = json.loads(response.read().decode())
			return confirmed["meta"]["height"]
	except urllib.error.HTTPError as err:
		if err.status != 400:
			raise
		return None  # [<step-2]


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
	)  # [<step-3]


def wait_for_confirmation(  # [>step-4]
	tx_hash, max_attempts=120, wait_seconds=1
):
	"""
	Check for confirmation repeatedly until the transaction is confirmed or
	the attempts run out.

	Args:
		tx_hash: hash of the transaction to monitor
		max_attempts: maximum polling attempts
		wait_seconds: seconds to wait between attempts

	Returns:
		True if the transaction was confirmed, False otherwise
	"""
	print("\nWaiting for transaction confirmation")
	for attempt in range(1, max_attempts + 1):
		time.sleep(wait_seconds)
		height = get_confirmation_height(tx_hash)
		status = f"confirmed in block {height}" if height else "pending"
		print(f"  Attempt {attempt}: {status}")
		if height:
			return True
	return False  # [<step-4]


try:  # [>step-5]
	block_height = get_confirmation_height(transaction_hash)
	if block_height:
		print(f"\nTransaction already confirmed in block {block_height}")
	elif not is_in_unconfirmed_pool(transaction_signature, signer_address):
		print("\nTransaction not in the unconfirmed pool")
	elif wait_for_confirmation(transaction_hash):
		print("\nTransaction confirmed!")
	else:
		print("\nTransaction not confirmed within the polling window")
except urllib.error.URLError as err:
	print(f"\nCould not reach the node: {err.reason}")  # [<step-5]

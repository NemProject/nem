import os

from symbolchain.CryptoTypes import PrivateKey
from symbolchain.facade.NemFacade import NemFacade

# Initialize the facade for the testnet network [>step-1]
facade = NemFacade('testnet')
# [<step-1]
# Use an existing private key if provided, [>step-2]
# Otherwise generate a random one.
private_key_string = os.getenv('PRIVATE_KEY')
if private_key_string:
	print('Loading account from environment variable...')
	private_key = PrivateKey(private_key_string)
else:
	print('Generating random account...')
	private_key = PrivateKey.random()
# [<step-2]
# Create a key pair from the private key [>step-3]
key_pair = facade.KeyPair(private_key)

# Derive the public key from the private key
public_key = key_pair.public_key

# Derive the address from the public key
address = facade.network.public_key_to_address(public_key)

# Output the account details
print(f'Address: {address}')
print(f'Public key: {public_key}')
print(f'Private key: {private_key}')  # [<step-3]

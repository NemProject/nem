import os

from symbolchain.Bip32 import Bip32
from symbolchain.facade.NemFacade import NemFacade

# Initialize the facade for the testnet network [>step-1]
facade = NemFacade('testnet')
# [<step-1]
# Use an existing mnemonic if provided, otherwise generate a random one [>step-2]
bip32 = Bip32(NemFacade.BIP32_CURVE_NAME)
mnemonic = os.getenv('MNEMONIC')
if mnemonic:
	print('Loading mnemonic phrase from environment variable...')
else:
	print('Generating random mnemonic phrase...')
	mnemonic = bip32.random()
print(f'Mnemonic phrase: {mnemonic}')
# [<step-2]
# Load password from environment variable or use default [>step-3]
password = os.getenv('PASSWORD', 'correcthorsebatterystaple')
print(f'Password: {password}')

# Derive a root Bip32 node from the mnemonic and a password
root_node = bip32.from_mnemonic(mnemonic, password)
# [<step-3]
# Derive a child Bip32 node for the account at index 0 [>step-4]
account_index = 0
child_node = root_node.derive_path(facade.bip32_path(account_index))
# [<step-4]
# Convert the Bip32 node to a signing key pair [>step-5]
key_pair = facade.bip32_node_to_key_pair(child_node)

# Derive the address from the public key
address = facade.network.public_key_to_address(key_pair.public_key)

# Output the account details
print(f'Address: {address}')
print(f'Public key: {key_pair.public_key}')
print(f'Private key: {key_pair.private_key}')  # [<step-5]

import { Bip32 } from 'symbol-sdk';
import { NemFacade } from 'symbol-sdk/nem';
import process from 'process';

// Initialize the facade for the testnet network [>step-1]
const facade = new NemFacade('testnet');
// [<step-1]
// Use an existing mnemonic if provided, otherwise generate a random one [>step-2]
const bip32 = new Bip32(NemFacade.BIP32_CURVE_NAME);
let mnemonic = process.env.MNEMONIC;
if (mnemonic) {
	console.log('Loading mnemonic phrase from environment variable...');
} else {
	console.log('Generating random mnemonic phrase...');
	mnemonic = bip32.random();
}
console.log('Mnemonic phrase:', mnemonic);
// [<step-2]
// Load password from environment variable or use default [>step-3]
const password = process.env.PASSWORD || 'correcthorsebatterystaple';
console.log('Password:', password);

// Derive a root Bip32 node from the mnemonic and a password
const rootNode = bip32.fromMnemonic(mnemonic, password);
// [<step-3]
// Derive a child Bip32 node for the account at index 0 [>step-4]
const accountIndex = 0;
const childNode = rootNode.derivePath(facade.bip32Path(accountIndex));
// [<step-4]
// Convert the Bip32 node to a signing key pair [>step-5]
const keyPair = NemFacade.bip32NodeToKeyPair(childNode);

// Derive the address from the public key
const address = facade.network.publicKeyToAddress(keyPair.publicKey);

// Output the account details
console.log('Address:', address.toString());
console.log('Public key:', keyPair.publicKey.toString());
console.log('Private key:', keyPair.privateKey.toString()); // [<step-5]

import { PrivateKey } from 'symbol-sdk';
import { NemFacade } from 'symbol-sdk/nem';
import process from 'process';

// Initialize the facade for the testnet network [>step-1]
const facade = new NemFacade('testnet');
// [<step-1]
// Use an existing private key if provided, [>step-2]
// Otherwise generate a random one.
const privateKeyString = process.env.PRIVATE_KEY;
let privateKey;
if (privateKeyString) {
	console.log('Loading account from environment variable...');
	privateKey = new PrivateKey(privateKeyString);
} else {
	console.log('Generating random account...');
	privateKey = PrivateKey.random();
} // [<step-2]
// [>step-3]
// Create a key pair from the private key
const keyPair = new NemFacade.KeyPair(privateKey);

// Derive the public key from the private key
const publicKey = keyPair.publicKey;

// Derive the address from the public key
const address = facade.network.publicKeyToAddress(publicKey);

// Output the account details
console.log('Address:', address.toString());
console.log('Public key:', publicKey.toString());
console.log('Private key:', privateKey.toString()); // [<step-3]

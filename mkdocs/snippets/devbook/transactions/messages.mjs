import { PrivateKey, PublicKey } from 'symbol-sdk';
import {
	Address,
	MessageEncoder,
	NemFacade,
	calculateTransactionFee,
	descriptors,
	models
} from 'symbol-sdk/nem';

// Configuration
const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log('Using node', NODE_URL);

// Helper function to poll for confirmed transaction
async function retrieveConfirmedTransaction(hash, label) {
	console.log(`Polling for ${label} confirmation...`);
	let attempts = 0;
	const maxAttempts = 120;

	while (attempts < maxAttempts) {
		const response = await fetch(
			`${NODE_URL}/transaction/get?hash=${hash}`);
		if (response.ok) {
			console.log(`  ${label} confirmed!`);
			return response.json();
		}
		attempts++;
		await new Promise(resolve => { setTimeout(resolve, 2000); });
	}

	throw new Error(
		`${label} not confirmed after ${maxAttempts} attempts`);
}

// Set up sender and recipient accounts [>step-1]
const facade = new NemFacade('testnet');

const senderPrivateKeyString = process.env.SENDER_PRIVATE_KEY ||
	'0000000000000000000000000000000000000000000000000000000000000000';
const senderKeyPair = new NemFacade.KeyPair(
	new PrivateKey(senderPrivateKeyString));
const senderAddress = facade.network.publicKeyToAddress(
	senderKeyPair.publicKey);

const recipientPrivateKeyString = process.env.RECIPIENT_PRIVATE_KEY ||
	'1111111111111111111111111111111111111111111111111111111111111111';
const recipientKeyPair = new NemFacade.KeyPair(
	new PrivateKey(recipientPrivateKeyString));
const recipientAddress = facade.network.publicKeyToAddress(
	recipientKeyPair.publicKey);

console.log('Sender address:', senderAddress.toString());
console.log('Recipient address:', recipientAddress.toString(), '\n');
// [<step-1]
// --- PLAIN TEXT MESSAGE ---
console.log('==> Sending Plain Text Message'); // [>step-2]

// Create a plain text message
const plainMessage = new TextEncoder().encode('Hello, NEM!');
console.log('Plain message:',
	new TextDecoder().decode(plainMessage));

// Build transfer transaction with plain message
const plainTransaction = facade.createTransactionFromTypedDescriptor(
	new descriptors.TransferTransactionV2Descriptor(
		new Address(recipientAddress.toString()),
		new models.Amount(0n),
		new descriptors.MessageDescriptor(
			models.MessageType.PLAIN,
			plainMessage)),
	senderKeyPair.publicKey,
	0n,
	2 * 60 * 60); // [<step-2]
plainTransaction.fee = new models.Amount(
	calculateTransactionFee(plainTransaction));

// Sign and announce the transaction
const plainSignature = facade.signTransaction(
	senderKeyPair, plainTransaction);
const plainJsonPayload = facade.transactionFactory.static
	.attachSignature(plainTransaction, plainSignature);
const plainTransactionHash = facade.hashTransaction(
	plainTransaction).toString();
console.log('Transaction hash:', plainTransactionHash);

await fetch(`${NODE_URL}/transaction/announce`, {
	method: 'POST',
	headers: { 'Content-Type': 'application/json' },
	body: plainJsonPayload
});
console.log('Plain message transaction announced\n');

// --- RECEIVING PLAIN TEXT MESSAGE ---
console.log('<== Receiving Plain Text Message'); // [>step-3]

// Wait for confirmation
const plainTxData = await retrieveConfirmedTransaction(
	plainTransactionHash, 'Plain message transaction');

// Decode plain message from confirmed transaction
const receivedPlainMessage = Buffer.from(
	plainTxData.transaction.message.payload, 'hex');
console.log('Received plain message:',
	new TextDecoder().decode(receivedPlainMessage), '\n');
// [<step-3]
// --- ENCRYPTED MESSAGE ---
console.log('==> Sending Encrypted Message'); // [>step-4]

// Create a message encoder with sender's key pair
const senderMessageEncoder = new MessageEncoder(senderKeyPair);

// Encrypt the message using recipient's public key
const secretMessage = new TextEncoder().encode(
	'This is a secret message!');
const encryptedMessage = senderMessageEncoder.encode(
	recipientKeyPair.publicKey, secretMessage
);
console.log('Original message:', new TextDecoder().decode(secretMessage));
console.log('Encrypted payload:',
	Buffer.from(encryptedMessage.message).toString('hex'));

// Build transfer transaction with encrypted message
const encryptedTransaction = facade.createTransactionFromTypedDescriptor(
	new descriptors.TransferTransactionV2Descriptor(
		new Address(recipientAddress.toString()),
		new models.Amount(0n),
		new descriptors.MessageDescriptor(
			models.MessageType.ENCRYPTED,
			encryptedMessage.message)),
	senderKeyPair.publicKey,
	0n,
	2 * 60 * 60); // [<step-4]
encryptedTransaction.fee = new models.Amount(
	calculateTransactionFee(encryptedTransaction));

// Sign and announce the transaction
const encryptedSignature = facade.signTransaction(
	senderKeyPair, encryptedTransaction);
const encryptedJsonPayload = facade.transactionFactory.static
	.attachSignature(encryptedTransaction, encryptedSignature);
const encryptedTransactionHash = facade.hashTransaction(
	encryptedTransaction).toString();
console.log('Transaction hash:', encryptedTransactionHash);

await fetch(`${NODE_URL}/transaction/announce`, {
	method: 'POST',
	headers: { 'Content-Type': 'application/json' },
	body: encryptedJsonPayload
});
console.log('Encrypted message transaction announced\n');

// --- RECEIVING ENCRYPTED MESSAGE ---
console.log('<== Receiving Encrypted Message'); // [>step-5]

// Wait for confirmation
const encryptedTxData = await retrieveConfirmedTransaction(
	encryptedTransactionHash, 'Encrypted message transaction');

// Decode encrypted message using recipient's private key
const recipientMessageEncoder = new MessageEncoder(recipientKeyPair);
const receivedEncryptedMessage = new models.Message();
receivedEncryptedMessage.messageType = models.MessageType.ENCRYPTED;
receivedEncryptedMessage.message = Buffer.from(
	encryptedTxData.transaction.message.payload, 'hex');

// Get sender's public key from the transaction
const senderPublicKeyFromTx = new PublicKey(
	encryptedTxData.transaction.signer);

const result = recipientMessageEncoder.tryDecode(
	senderPublicKeyFromTx, receivedEncryptedMessage);

if (result.isDecoded) {
	console.log('Recipient decrypted message:',
		new TextDecoder().decode(result.message));
} else {
	console.log('Recipient failed to decrypt message');
} // [<step-5]

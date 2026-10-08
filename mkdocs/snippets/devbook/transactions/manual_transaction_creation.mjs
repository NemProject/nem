import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade,
	NetworkTimestamp,
	calculateTransactionFee,
	models
} from 'symbol-sdk/nem';

const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log('Using node', NODE_URL);

// Helper function to announce a transaction
async function announceTransaction(payload, label) {
	const announcePath = '/transaction/announce';
	console.log(`Announcing ${label} to ${announcePath}`);
	const response = await fetch(`${NODE_URL}${announcePath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: payload
	});
	if (!response.ok)
		throw new Error(`HTTP ${response.status}`);
	const result = await response.json();
	console.log('  Result:', result.message);
	return result.message;
}

// Helper function to wait for transaction confirmation
async function waitForConfirmation(transactionHash, label) {
	const statusPath = `/transaction/get?hash=${transactionHash}`;
	console.log(`Waiting for ${label} confirmation from`, statusPath);
	let isConfirmed = false;
	for (let attempt = 1; 120 >= attempt; ++attempt) {
		const response = await fetch(`${NODE_URL}${statusPath}`);
		if (!response.ok) {
			console.log('  Transaction status: pending');
			await new Promise(resolve => { setTimeout(resolve, 1000); });
		} else {
			const confirmed = await response.json();
			console.log(`${label} confirmed in block`,
				confirmed.meta.height);
			isConfirmed = true;
			break;
		}
	}
	if (!isConfirmed)
		console.warn(`${label} confirmation took too long.`);
}

// [>step-1]
const SIGNER_PRIVATE_KEY = process.env.SIGNER_PRIVATE_KEY ||
	'0000000000000000000000000000000000000000000000000000000000000000';
const signerKeyPair = new NemFacade.KeyPair(
	new PrivateKey(SIGNER_PRIVATE_KEY));

const RECIPIENT_ADDRESS = process.env.RECIPIENT_ADDRESS ||
	'TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4';
// [<step-1]
const facade = new NemFacade('testnet');

// Define the amount of XEM to transfer [>step-2]
const xem = parseFloat(process.env.XEM_AMOUNT || '1');
const amount = BigInt(Math.round(xem * 1_000_000));
// [<step-2]

try {
	// Fetch current network time [>step-3]
	const timePath = '/time-sync/network-time';
	console.log('Fetching current network time from', timePath);
	const timeResponse = await fetch(`${NODE_URL}${timePath}`);
	const timeJSON = await timeResponse.json();
	const networkTime = Math.floor(timeJSON.receiveTimeStamp / 1000);
	console.log('  Network time:', networkTime,
		's since the nemesis block');

	// Derived fields from network time
	const timestamp = new NetworkTimestamp(networkTime);
	const deadline = timestamp.addHours(2);
	// [<step-3]
	// Build the transaction [>step-4]
	const transaction = facade.transactionFactory.create({
		type: 'transfer_transaction_v2',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		recipientAddress: RECIPIENT_ADDRESS,
		amount
	});
	// [<step-4]
	// Calculate and attach the transaction fee [>step-5]
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	// [<step-5]
	// Sign transaction and generate final payload [>step-6]
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built transaction:');
	console.dir(transaction.toJson(), { colors: true });
	// [<step-6]
	// Announce the transaction [>step-7]
	const announceResult = await announceTransaction(
		jsonPayload, 'transaction');
	// [<step-7]
	// Wait for confirmation [>step-8]
	if ('SUCCESS' === announceResult) {
		const transactionHash = facade.hashTransaction(transaction)
			.toString();
		console.log('Transaction hash:', transactionHash);
		await waitForConfirmation(transactionHash, 'transaction');
	} else {
		console.log('Transaction rejected:', announceResult);
	}
	// [<step-8]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

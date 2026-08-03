import { PrivateKey, PublicKey } from 'symbol-sdk';
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
	const announceResponse = await fetch(`${NODE_URL}${announcePath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: payload
	});
	const result = await announceResponse.json();
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
		if (response.ok) {
			const confirmed = await response.json();
			console.log(`${label} confirmed in block`,
				confirmed.meta.height);
			isConfirmed = true;
			break;
		}
		console.log('  Transaction status: pending');
		await new Promise(resolve => { setTimeout(resolve, 1000); });
	}
	if (!isConfirmed)
		console.warn(`${label} confirmation took too long.`);
}

const facade = new NemFacade('testnet');
// [>step-1]
const MULTISIG_PUBLIC_KEY = process.env.MULTISIG_PUBLIC_KEY || (
	'D656155B48D4E71E4C59EC6FAEB5EB4F214DE8BC3C65D5BF6A3D9931B4E5ACF2');
const multisigPublicKey = new PublicKey(MULTISIG_PUBLIC_KEY);
const multisigAddress = facade.network.publicKeyToAddress(
	multisigPublicKey);
console.log(`Multisig public key: ${multisigPublicKey}`);
const COSIGNATORY0_PRIVATE_KEY = process.env.COSIGNATORY0_PRIVATE_KEY || (
	'0000000000000000000000000000000000000000000000000000000000000002');
const cosignatory0KeyPair = new NemFacade.KeyPair(
	new PrivateKey(COSIGNATORY0_PRIVATE_KEY));
console.log(`Cosignatory 0 public key: ${cosignatory0KeyPair.publicKey}`);
const COSIGNATORY1_PRIVATE_KEY = process.env.COSIGNATORY1_PRIVATE_KEY || (
	'0000000000000000000000000000000000000000000000000000000000000003');
const cosignatory1KeyPair = new NemFacade.KeyPair(
	new PrivateKey(COSIGNATORY1_PRIVATE_KEY));
console.log(`Cosignatory 1 public key: ${cosignatory1KeyPair.publicKey}`);
// [<step-1]

try {
	// Fetch current network time [>step-2]
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
	// [<step-2]
	// Build the inner transfer transaction [>step-3]
	const transferTransaction = facade.transactionFactory.create({
		type: 'transfer_transaction_v2',
		signerPublicKey: multisigPublicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		recipientAddress: multisigAddress.toString(),
		amount: 1_000_000n // 1 XEM
	});
	transferTransaction.fee = new models.Amount(
		calculateTransactionFee(transferTransaction));
	// [<step-3]
	// Build the wrapper multisig transaction [>step-4]
	const transaction = facade.transactionFactory.create({
		type: 'multisig_transaction_v1',
		// This is the cosignatory that initiates the transfer
		signerPublicKey: cosignatory0KeyPair.publicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		innerTransaction: facade.transactionFactory.static
			.toNonVerifiableTransaction(transferTransaction)
	});
	transaction.fee = new models.Amount(
		calculateTransactionFee(transaction));
	// [<step-4]
	// Sign and announce the multisig transaction [>step-5]
	const signature = facade.signTransaction(
		cosignatory0KeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built multisig transaction:');
	console.log(JSON.stringify(transaction.toJson(), null, 2));
	const announceResult = await announceTransaction(
		jsonPayload, 'multisig transaction');
	// The transaction is now waiting for the second signature
	// [<step-5]
	// Retrieve the pending transaction from the network [>step-6]
	if ('SUCCESS' === announceResult) {
		const cosignatory1Address = facade.network.publicKeyToAddress(
			cosignatory1KeyPair.publicKey);
		const unconfirmedPath = '/account/unconfirmedTransactions' +
			`?address=${cosignatory1Address}`;
		console.log('Fetching pending transactions from',
			unconfirmedPath);
		const unconfirmedResponse = await fetch(
			`${NODE_URL}${unconfirmedPath}`);
		const pending = (await unconfirmedResponse.json()).data;
		// Select the pending transaction issued by the multisig account
		const pendingEntry = pending.find(entry =>
			multisigPublicKey.toString() === (entry.transaction
				.otherTrans?.signer ?? '').toUpperCase());
		const innerTransactionHash = pendingEntry.meta.data;
		console.log('  Inner transaction hash:', innerTransactionHash);
		// [<step-6]
		// Build the cosignature [>step-7]
		const cosignature = facade.transactionFactory.create({
			type: 'cosignature_v1',
			// This is the cosignatory providing the second signature
			signerPublicKey: cosignatory1KeyPair.publicKey.toString(),
			timestamp: timestamp.timestamp,
			deadline: deadline.timestamp,
			// Hash of the inner transfer transaction
			otherTransactionHash: innerTransactionHash,
			// Address of the multisig account
			multisigAccountAddress: multisigAddress.toString()
		});
		cosignature.fee = new models.Amount(
			calculateTransactionFee(cosignature));
		// [<step-7]
		// Sign and announce the cosignature [>step-8]
		const cosignatureSignature = facade.signTransaction(
			cosignatory1KeyPair, cosignature);
		const cosignaturePayload = facade.transactionFactory.static
			.attachSignature(cosignature, cosignatureSignature);
		console.log('Built cosignature:');
		console.log(JSON.stringify(cosignature.toJson(), null, 2));
		const cosignatureResult = await announceTransaction(
			cosignaturePayload, 'cosignature');
		// [<step-8]
		// Wait for the multisig transaction to be confirmed [>step-9]
		if ('SUCCESS' === cosignatureResult) {
			await waitForConfirmation(
				facade.hashTransaction(transaction).toString(),
				'multisig transaction');
		} else {
			console.log('Transaction rejected:', cosignatureResult);
		}
		// [<step-9]
	} else {
		console.log('Transaction rejected:', announceResult);
	}
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

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

const facade = new NemFacade('testnet');
// [>step-1]
const KEY_PREFIX = '0'.repeat(63);

// Set up the keys for the multisig account and its two cosignatories
const MULTISIG_PRIVATE_KEY = process.env.MULTISIG_PRIVATE_KEY || (
	`${KEY_PREFIX}1`);
const multisigKeyPair = new NemFacade.KeyPair(
	new PrivateKey(MULTISIG_PRIVATE_KEY));
const multisigAddress = facade.network.publicKeyToAddress(
	multisigKeyPair.publicKey);
console.log(`Multisig address: ${multisigAddress}`,
	`(public key ${multisigKeyPair.publicKey})`);

const cosignatoryKeyPairs = [];
for (let i = 0; 2 > i; i++) {
	const COSIGNATORY_PRIVATE_KEY =
		process.env[`COSIGNATORY${i}_PRIVATE_KEY`] || (
			KEY_PREFIX + String(i + 2));
	const keyPair = new NemFacade.KeyPair(
		new PrivateKey(COSIGNATORY_PRIVATE_KEY));
	cosignatoryKeyPairs.push(keyPair);
	const addr = facade.network.publicKeyToAddress(keyPair.publicKey);
	console.log(`Cosignatory ${i} address: ${addr}`,
		`(public key ${keyPair.publicKey})`);
}
// [<step-1]
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

// Returns the cosignatory addresses of the provided multisig [>step-3]
// account, or an empty list if the account is not multisig
async function getMultisigCosignatories(address) {
	const accountPath = `/account/get?address=${address}`;
	console.log(`Getting cosignatories from ${accountPath}`);
	const response = await fetch(`${NODE_URL}${accountPath}`);
	const accountInfo = await response.json();
	const foundCosignatories = accountInfo.meta.cosignatories
		.map(cosignatory => cosignatory.address);
	if (0 === foundCosignatories.length) {
		console.log('  Response: No cosignatories');
		return [];
	}
	console.log('  Response:', JSON.stringify(foundCosignatories));
	return foundCosignatories;
}
// [<step-3]
// [>step-5]
// Returns a transaction that turns a regular account into a multisig
function multisigEnableTransaction(timestamp, deadline, approvalDelta) {
	// Create a multisig account modification transaction
	// that adds the cosignatories
	const modifications = cosignatoryKeyPairs.map(keyPair => ({
		modification: {
			modificationType: 'add_cosignatory',
			cosignatoryPublicKey: keyPair.publicKey.toString()
		}
	}));
	const transaction = facade.transactionFactory.create({
		type: 'multisig_account_modification_transaction_v2',
		// This is the account that will be turned into a multisig
		signerPublicKey: multisigKeyPair.publicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		// Change of the number of cosignatures
		// required to approve transactions
		minApprovalDelta: approvalDelta,
		modifications
	});
	// [<step-5]
	// Calculate and attach the transaction fee [>step-6]
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	console.log(
		'Enabling the multisig with the modification transaction:');
	console.log(JSON.stringify(transaction.toJson(), null, 2));
	// [<step-6]
	// Sign the transaction with the multisig's key [>step-7]
	const signature = facade.signTransaction(
		multisigKeyPair, transaction);
	facade.transactionFactory.static.attachSignature(
		transaction, signature);
	return transaction; // [<step-7]
}

// [>step-8]
// Returns a transaction that removes one cosignatory from the multisig
function multisigRemovalTransaction(timestamp, deadline,
	removedKeyPair, approvalDelta) {
	// Create a multisig account modification transaction
	// that removes a single cosignatory
	const innerTransaction = facade.transactionFactory.create({
		type: 'multisig_account_modification_transaction_v2',
		// This is the multisig account that will be modified
		signerPublicKey: multisigKeyPair.publicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		// Change of the number of cosignatures
		// required to approve transactions
		minApprovalDelta: approvalDelta,
		modifications: [
			{
				modification: {
					modificationType: 'delete_cosignatory',
					cosignatoryPublicKey:
						removedKeyPair.publicKey.toString()
				}
			}
		]
	});
	// [<step-8]
	// Wrap the modification in a multisig transaction [>step-9]
	const innerFee = calculateTransactionFee(innerTransaction);
	innerTransaction.fee = new models.Amount(innerFee);
	const transaction = facade.transactionFactory.create({
		type: 'multisig_transaction_v1',
		// This is the cosignatory that initiates the removal
		signerPublicKey: cosignatoryKeyPairs[0].publicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		innerTransaction: facade.transactionFactory.static
			.toNonVerifiableTransaction(innerTransaction)
	});
	// [<step-9]
	// Calculate and attach the transaction fee [>step-10]
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log('  Transaction fee:',
		`${Number(innerFee + fee) / 1_000_000} XEM`);
	console.log(
		'Disabling the multisig with the multisig transaction:');
	console.log(JSON.stringify(transaction.toJson(), null, 2));
	// [<step-10]
	// Sign the transaction with the cosignatory's key [>step-11]
	const signature = facade.signTransaction(
		cosignatoryKeyPairs[0], transaction);
	facade.transactionFactory.static
		.attachSignature(transaction, signature);
	return transaction; // [<step-11]
}

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
	// Get current state of the multisig account and decide [>step-4]
	// which operation to perform
	const cosignatories = await getMultisigCosignatories(multisigAddress);
	let transactions;
	if (0 === cosignatories.length) {
		// Enable the multisig
		transactions = [multisigEnableTransaction(
			timestamp, deadline, 1)];
	} else {
		// Disable the multisig
		transactions = [
			multisigRemovalTransaction(
				timestamp, deadline, cosignatoryKeyPairs[1], 0),
			multisigRemovalTransaction(
				timestamp, deadline, cosignatoryKeyPairs[0], -1)
		];
	}
	// [<step-4]
	// Announce each transaction and wait for confirmation [>step-12]
	for (const signedTransaction of transactions) {
		const transactionHash = facade.hashTransaction(signedTransaction)
			.toString();
		console.log('Built transaction with hash:', transactionHash);
		const jsonPayload = facade.transactionFactory.static
			.toJson(signedTransaction);
		const result = await announceTransaction(
			jsonPayload, 'transaction');
		if ('SUCCESS' !== result) {
			console.log('Transaction rejected');
			break;
		}
		await waitForConfirmation(transactionHash, 'transaction');
	}
	// [<step-12]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

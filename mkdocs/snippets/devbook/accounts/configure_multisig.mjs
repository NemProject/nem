import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade,
	calculateTransactionFee,
	descriptors,
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

// Returns the cosignatory addresses of the provided multisig [>step-2]
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
// [<step-2]
// [>step-4]
// Returns a transaction that turns a regular account into a multisig
function multisigEnableTransaction(approvalDelta) {
	// Create a multisig account modification transaction
	// that adds the cosignatories
	const modifications = cosignatoryKeyPairs.map(keyPair =>
		new descriptors.SizePrefixedMultisigAccountModificationDescriptor(
			new descriptors.MultisigAccountModificationDescriptor(
				models.MultisigAccountModificationType.ADD_COSIGNATORY,
				keyPair.publicKey)));
	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.MultisigAccountModificationTransactionV2Descriptor(
		// Change of the number of cosignatures
		// required to approve transactions
			approvalDelta,
			modifications),
		// This is the account that will be turned into a multisig
		multisigKeyPair.publicKey,
		0n,
		2 * 60 * 60);
	// [<step-4]
	// Calculate and attach the transaction fee [>step-5]
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	console.log(
		'Enabling the multisig with the modification transaction:');
	console.log(JSON.stringify(transaction.toJson(), null, 2));
	// [<step-5]
	// Sign the transaction with the multisig's key [>step-6]
	const signature = facade.signTransaction(
		multisigKeyPair, transaction);
	facade.transactionFactory.static.attachSignature(
		transaction, signature);
	return transaction; // [<step-6]
}

// [>step-7]
// Returns a transaction that removes one cosignatory from the multisig
function multisigRemovalTransaction(removedKeyPair, approvalDelta) {
	// Create a multisig account modification transaction
	// that removes a single cosignatory
	const innerTransaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.MultisigAccountModificationTransactionV2Descriptor(
		// Change of the number of cosignatures
		// required to approve transactions
			approvalDelta,
			[new descriptors.SizePrefixedMultisigAccountModificationDescriptor(
				new descriptors.MultisigAccountModificationDescriptor(
					models.MultisigAccountModificationType.DELETE_COSIGNATORY,
					removedKeyPair.publicKey))]),
		// This is the multisig account that will be modified
		multisigKeyPair.publicKey,
		0n,
		2 * 60 * 60);
	// [<step-7]
	// Wrap the modification in a multisig transaction [>step-8]
	const innerFee = calculateTransactionFee(innerTransaction);
	innerTransaction.fee = new models.Amount(innerFee);
	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.MultisigTransactionV1Descriptor(
			facade.transactionFactory.static.toNonVerifiableTransaction(
				innerTransaction)),
		// This is the cosignatory that initiates the removal
		cosignatoryKeyPairs[0].publicKey,
		0n,
		2 * 60 * 60);
	// [<step-8]
	// Calculate and attach the transaction fee [>step-9]
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log('  Transaction fee:',
		`${Number(innerFee + fee) / 1_000_000} XEM`);
	console.log(
		'Disabling the multisig with the multisig transaction:');
	console.log(JSON.stringify(transaction.toJson(), null, 2));
	// [<step-9]
	// Sign the transaction with the cosignatory's key [>step-10]
	const signature = facade.signTransaction(
		cosignatoryKeyPairs[0], transaction);
	facade.transactionFactory.static
		.attachSignature(transaction, signature);
	return transaction; // [<step-10]
}

try {
	// Get current state of the multisig account and decide [>step-3]
	// which operation to perform
	const cosignatories = await getMultisigCosignatories(multisigAddress);
	let transactions;
	if (0 === cosignatories.length) {
		// Enable the multisig
		transactions = [multisigEnableTransaction(1)];
	} else {
		// Disable the multisig
		transactions = [
			multisigRemovalTransaction(cosignatoryKeyPairs[1], 0),
			multisigRemovalTransaction(cosignatoryKeyPairs[0], -1)
		];
	}
	// [<step-3]
	// Announce each transaction and wait for confirmation [>step-11]
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
	// [<step-11]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

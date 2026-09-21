import { Hash256, PrivateKey, PublicKey } from 'symbol-sdk';
import {
	Address,
	NemFacade,
	calculateTransactionFee,
	descriptors,
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
	// Build the inner transfer transaction [>step-2]
	const transferTransaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.TransferTransactionV2Descriptor(
			new Address(multisigAddress.toString()),
			new models.Amount(1_000_000n)), // 1 XEM
		multisigPublicKey,
		0n,
		2 * 60 * 60);
	transferTransaction.fee = new models.Amount(
		calculateTransactionFee(transferTransaction));
	// [<step-2]
	// Build the wrapper multisig transaction [>step-3]
	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.MultisigTransactionV1Descriptor(
			facade.transactionFactory.static.toNonVerifiableTransaction(
				transferTransaction)),
		// This is the cosignatory that initiates the transfer
		cosignatory0KeyPair.publicKey,
		0n,
		2 * 60 * 60);
	transaction.fee = new models.Amount(
		calculateTransactionFee(transaction));
	// [<step-3]
	// Sign and announce the multisig transaction [>step-4]
	const signature = facade.signTransaction(
		cosignatory0KeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built multisig transaction:');
	console.log(JSON.stringify(transaction.toJson(), null, 2));
	const announceResult = await announceTransaction(
		jsonPayload, 'multisig transaction');
	// The transaction is now waiting for the second signature
	// [<step-4]
	// Retrieve the pending transaction from the network [>step-5]
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
		// [<step-5]
		// Build the cosignature [>step-6]
		const cosignature = facade.createTransactionFromTypedDescriptor(
			new descriptors.CosignatureV1Descriptor(
				// Hash of the inner transfer transaction
				new Hash256(innerTransactionHash),
				// Address of the multisig account
				new Address(multisigAddress.toString())),
			// This is the cosignatory providing the second signature
			cosignatory1KeyPair.publicKey,
			0n,
			2 * 60 * 60);
		cosignature.fee = new models.Amount(
			calculateTransactionFee(cosignature));
		// [<step-6]
		// Sign and announce the cosignature [>step-7]
		const cosignatureSignature = facade.signTransaction(
			cosignatory1KeyPair, cosignature);
		const cosignaturePayload = facade.transactionFactory.static
			.attachSignature(cosignature, cosignatureSignature);
		console.log('Built cosignature:');
		console.log(JSON.stringify(cosignature.toJson(), null, 2));
		const cosignatureResult = await announceTransaction(
			cosignaturePayload, 'cosignature');
		// [<step-7]
		// Wait for the multisig transaction to be confirmed [>step-8]
		if ('SUCCESS' === cosignatureResult) {
			await waitForConfirmation(
				facade.hashTransaction(transaction).toString(),
				'multisig transaction');
		} else {
			console.log('Transaction rejected:', cosignatureResult);
		}
		// [<step-8]
	} else {
		console.log('Transaction rejected:', announceResult);
	}
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

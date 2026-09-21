import { PrivateKey } from 'symbol-sdk';
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
	// Build the transaction [>step-3]
	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.TransferTransactionV2Descriptor(
			new Address(RECIPIENT_ADDRESS),
			new models.Amount(amount)),
		signerKeyPair.publicKey,
		0n,
		2 * 60 * 60);
	// [<step-3]
	// Calculate and attach the transaction fee [>step-4]
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	// [<step-4]
	// Sign transaction and generate final payload [>step-5]
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built transaction:');
	console.dir(transaction.toJson(), { colors: true });
	// [<step-5]
	// Announce the transaction [>step-6]
	const announcePath = '/transaction/announce';
	console.log('Announcing transaction to', announcePath);
	const announceResponse = await fetch(`${NODE_URL}${announcePath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await announceResponse.json();
	console.log('  Result:', announceResult.message);
	// [<step-6]
	// Wait for confirmation [>step-7]
	if ('SUCCESS' === announceResult.message) {
		const transactionHash = facade.hashTransaction(transaction)
			.toString();
		const statusPath = `/transaction/get?hash=${transactionHash}`;
		console.log('Waiting for confirmation from', statusPath);

		let isConfirmed = false;
		for (let attempt = 1; 120 >= attempt; ++attempt) {
			const response = await fetch(`${NODE_URL}${statusPath}`);

			if (response.ok) {
				const confirmed = await response.json();
				console.log('Transaction confirmed in block',
					confirmed.meta.height);
				isConfirmed = true;
				break;
			}
			console.log('  Transaction status: pending');
			await new Promise(resolve => { setTimeout(resolve, 1000); });
		}
		if (!isConfirmed)
			console.warn('Confirmation took too long.');
	} else {
		console.log('Transaction rejected:', announceResult.message);
	}
	// [<step-7]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

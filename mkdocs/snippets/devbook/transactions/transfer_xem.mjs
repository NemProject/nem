import { PrivateKey } from 'symbol-sdk';
import { NemFacade } from 'symbol-sdk/nem';

const NODE_URL = 'http://libertalia.nemtest.net:7890';
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
const xem = parseInt(process.env.XEM_AMOUNT || '1', 10);
// Convert XEM to atomic units (XEM has a divisibility of 6)
const amount = BigInt(xem * 1_000_000);
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
	const timestamp = networkTime;
	const deadline = networkTime + (2 * 60 * 60);
	// [<step-3]
	// Calculate the transaction fee [>step-4]
	const feeSteps = Math.max(1, Math.min(25, Math.floor(xem / 10_000)));
	const fee = BigInt(feeSteps * 50_000);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	// [<step-4]
	// Build the transaction [>step-5]
	const transaction = facade.transactionFactory.create({
		type: 'transfer_transaction_v2',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		fee,
		timestamp,
		deadline,
		recipientAddress: RECIPIENT_ADDRESS,
		amount
	});
	// [<step-5]
	// Sign transaction and generate final payload [>step-6]
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built transaction:');
	console.dir(transaction.toJson(), { colors: true });
	// [<step-6]
	// Announce the transaction [>step-7]
	const announcePath = '/transaction/announce';
	console.log('Announcing transaction to', announcePath);
	const announceResponse = await fetch(`${NODE_URL}${announcePath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await announceResponse.json();
	console.log('  Result:', announceResult.message);
	// [<step-7]
	// Wait for confirmation [>step-8]
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
	// [<step-8]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

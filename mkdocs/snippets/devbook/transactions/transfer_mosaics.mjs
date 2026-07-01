import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade,
	calculateTransactionFee,
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
// [>step-2]
const MOSAIC_ID = process.env.MOSAIC_ID || 'company:token';
const [MOSAIC_NAMESPACE, MOSAIC_NAME] = MOSAIC_ID.split(':');
const QUANTITY = parseInt(process.env.QUANTITY || '100', 10);
console.log('Sending mosaic', MOSAIC_ID);
console.log(`  Amount: ${QUANTITY} units`);
// [<step-2]
const facade = new NemFacade('testnet');

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
	// Fetch the mosaic's divisibility and supply [>step-4]
	const definitionPath = `/mosaic/definition?mosaicId=${MOSAIC_ID}`;
	console.log('Fetching mosaic definition from', definitionPath);
	const definitionResponse =
		await fetch(`${NODE_URL}${definitionPath}`);
	const definition = await definitionResponse.json();
	const properties = Object.fromEntries(
		definition.properties.map(
			property => [property.name, property.value]));
	const divisibility = parseInt(properties.divisibility, 10);

	const supplyPath = `/mosaic/supply?mosaicId=${MOSAIC_ID}`;
	console.log('Fetching mosaic supply from', supplyPath);
	const supplyResponse = await fetch(`${NODE_URL}${supplyPath}`);
	const { supply } = await supplyResponse.json();
	console.log(`  ${MOSAIC_ID}: divisibility ${divisibility},`,
		`supply ${supply}`);
	// [<step-4]
	// Build the transaction [>step-5]
	const atomicQuantity = QUANTITY * (10 ** divisibility);
	const multiplier = 1;
	const scaledMultiplier = multiplier * 1_000_000;
	const transaction = facade.transactionFactory.create({
		type: 'transfer_transaction_v2',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		timestamp,
		deadline,
		recipientAddress: RECIPIENT_ADDRESS,
		amount: BigInt(scaledMultiplier),
		mosaics: [{
			mosaic: {
				mosaicId: {
					namespaceId: {
						name: MOSAIC_NAMESPACE
					},
					name: MOSAIC_NAME
				},
				amount: BigInt(atomicQuantity)
			}
		}]
	});
	// [<step-5]
	// Calculate and attach the transaction fee [>step-6]
	const fee = calculateTransactionFee(transaction, {
		[MOSAIC_ID]: { supply: BigInt(supply), divisibility }
	});
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	// [<step-6]
	// Sign transaction and generate final payload [>step-7]
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built transaction:');
	console.dir(transaction.toJson(), { colors: true, depth: null });
	// [<step-7]
	// Announce the transaction [>step-8]
	const announcePath = '/transaction/announce';
	console.log('Announcing transaction to', announcePath);
	const announceResponse = await fetch(`${NODE_URL}${announcePath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await announceResponse.json();
	console.log('  Result:', announceResult.message);
	// [<step-8]
	// Wait for confirmation [>step-9]
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
	// [<step-9]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

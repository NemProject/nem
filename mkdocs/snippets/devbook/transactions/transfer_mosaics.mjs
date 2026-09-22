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
// [>step-2]
const MOSAIC_ID = process.env.MOSAIC_ID || 'company:token';
const [MOSAIC_NAMESPACE, MOSAIC_NAME] = MOSAIC_ID.split(':');
const QUANTITY = parseInt(process.env.QUANTITY || '100', 10);
console.log('Sending mosaic', MOSAIC_ID);
console.log(`  Amount: ${QUANTITY} units`);
// [<step-2]
const facade = new NemFacade('testnet');

try {
	// Fetch the mosaic's divisibility and supply [>step-3]
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
	// [<step-3]
	// Build the transaction [>step-4]
	const atomicQuantity = QUANTITY * (10 ** divisibility);
	const multiplier = 1;
	const scaledMultiplier = multiplier * 1_000_000;
	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.TransferTransactionV2Descriptor(
			new Address(RECIPIENT_ADDRESS),
			new models.Amount(BigInt(scaledMultiplier)),
			undefined,
			[new descriptors.SizePrefixedMosaicDescriptor(
				new descriptors.MosaicDescriptor(
					new descriptors.MosaicIdDescriptor(
						new descriptors.NamespaceIdDescriptor(
							MOSAIC_NAMESPACE),
						MOSAIC_NAME),
					new models.Amount(BigInt(atomicQuantity))))]),
		signerKeyPair.publicKey,
		0n,
		2 * 60 * 60);
	// [<step-4]
	// Calculate and attach the transaction fee [>step-5]
	const fee = calculateTransactionFee(transaction, {
		[MOSAIC_ID]: { supply: BigInt(supply), divisibility }
	});
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	// [<step-5]
	// Sign transaction and generate final payload [>step-6]
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built transaction:');
	console.dir(transaction.toJson(), { colors: true, depth: null });
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

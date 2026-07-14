import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade,
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

// Helper function to fetch the current mosaic supply
async function fetchSupply(mosaic) {
	const supplyPath = `/mosaic/supply?mosaicId=${mosaic}`;
	const supplyResponse = await fetch(`${NODE_URL}${supplyPath}`);
	const supplyInfo = await supplyResponse.json();
	return supplyInfo.supply;
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
// [>step-1]
const SIGNER_PRIVATE_KEY = process.env.SIGNER_PRIVATE_KEY ||
	'0000000000000000000000000000000000000000000000000000000000000000';
const signerKeyPair = new NemFacade.KeyPair(
	new PrivateKey(SIGNER_PRIVATE_KEY));

const facade = new NemFacade('testnet');
const signerAddress = facade.network.publicKeyToAddress(
	signerKeyPair.publicKey);
console.log('Signer address:', signerAddress.toString());

const namespaceName = process.env.NAMESPACE || 'my_namespace';
const mosaicName = process.env.MOSAIC || 'token';
const mosaicId = `${namespaceName}:${mosaicName}`;
console.log('Mosaic ID:', mosaicId);
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
	const timestamp = networkTime;
	const deadline = networkTime + (2 * 60 * 60);
	// [<step-2]
	// --- INCREASING SUPPLY (MINTING) ---
	console.log('\n--- Increasing supply (minting) ---');
	// [>step-3]
	console.log('Supply before minting:', await fetchSupply(mosaicId));

	const increaseTx = facade.transactionFactory.create({
		type: 'mosaic_supply_change_transaction_v1',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		timestamp,
		deadline,
		mosaicId: {
			namespaceId: { name: namespaceName },
			name: mosaicName
		},
		action: 'increase',
		delta: 500n
	});
	increaseTx.fee = new models.Amount(
		calculateTransactionFee(increaseTx));

	const increaseSignature = facade.signTransaction(
		signerKeyPair, increaseTx);
	const increasePayload = facade.transactionFactory.static
		.attachSignature(increaseTx, increaseSignature);
	console.log('Built supply increase transaction:');
	console.dir(increaseTx.toJson(), { colors: true });
	const increaseResult = await announceTransaction(
		increasePayload, 'supply increase');
	if ('SUCCESS' === increaseResult) {
		await waitForConfirmation(
			facade.hashTransaction(increaseTx).toString(),
			'supply increase');
		console.log('Supply after minting:', await fetchSupply(mosaicId));
	} else {
		console.log('Supply increase rejected');
	}
	// [<step-3]
	// --- DECREASING SUPPLY (BURNING) ---
	console.log('\n--- Decreasing supply (burning) ---');
	// [>step-4]
	const decreaseTx = facade.transactionFactory.create({
		type: 'mosaic_supply_change_transaction_v1',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		timestamp,
		deadline,
		mosaicId: {
			namespaceId: { name: namespaceName },
			name: mosaicName
		},
		action: 'decrease',
		delta: 500n
	});
	decreaseTx.fee = new models.Amount(
		calculateTransactionFee(decreaseTx));

	const decreaseSignature = facade.signTransaction(
		signerKeyPair, decreaseTx);
	const decreasePayload = facade.transactionFactory.static
		.attachSignature(decreaseTx, decreaseSignature);
	console.log('Built supply decrease transaction:');
	console.dir(decreaseTx.toJson(), { colors: true });
	const decreaseResult = await announceTransaction(
		decreasePayload, 'supply decrease');
	if ('SUCCESS' === decreaseResult) {
		await waitForConfirmation(
			facade.hashTransaction(decreaseTx).toString(),
			'supply decrease');
		console.log('Supply after burning:', await fetchSupply(mosaicId));
	} else {
		console.log('Supply decrease rejected');
	}
	// [<step-4]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

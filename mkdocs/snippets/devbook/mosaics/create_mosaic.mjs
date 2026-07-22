import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade,
	NetworkTimestamp,
	calculateMosaicRentalFee,
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

const facade = new NemFacade('testnet');
const signerAddress = facade.network.publicKeyToAddress(
	signerKeyPair.publicKey);
console.log('Signer address:', signerAddress.toString());
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
	// Build the mosaic ID [>step-3]
	const namespaceName = process.env.NAMESPACE || 'my_namespace';
	const mosaicName = process.env.MOSAIC ||
		`token_${Math.floor(Date.now() / 1000)}`;
	const mosaicId = `${namespaceName}:${mosaicName}`;
	console.log('Creating mosaic:', mosaicId);
	// [<step-3]
	// Define the mosaic [>step-4]
	const mosaicDefinition = {
		ownerPublicKey: signerKeyPair.publicKey.toString(),
		id: {
			namespaceId: { name: namespaceName },
			name: mosaicName
		},
		description: 'My tutorial mosaic',
		properties: [
			{ property: { name: 'divisibility', value: '2' } },
			{ property: { name: 'initialSupply', value: '1000' } },
			{ property: { name: 'supplyMutable', value: 'true' } },
			{ property: { name: 'transferable', value: 'true' } }
		]
	};
	// [<step-4]
	// Build the mosaic definition transaction [>step-5]
	const rentalFee = calculateMosaicRentalFee();
	console.log('  Mosaic creation fee:',
		`${Number(rentalFee) / 1_000_000} XEM`);

	const transaction = facade.transactionFactory.create({
		type: 'mosaic_definition_transaction_v1',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		rentalFeeSink: 'TBMOSAICOD4F54EE5CDMR23CCBGOAM2XSJBR5OLC',
		rentalFee,
		mosaicDefinition
	});
	// [<step-5]
	// Calculate and attach the transaction fee [>step-6]
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log('  Transaction fee:', `${Number(fee) / 1_000_000} XEM`);
	// [<step-6]
	// Sign and generate final payload [>step-7]
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built mosaic definition transaction:');
	console.dir(transaction.toJson(), { colors: true });

	// Announce the transaction
	const announcePath = '/transaction/announce';
	console.log('Announcing mosaic definition to', announcePath);
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
	// Retrieve the mosaic [>step-9]
	const definitionPath = `/mosaic/definition?mosaicId=${mosaicId}`;
	console.log('Fetching mosaic information from', definitionPath);
	const definitionResponse = await fetch(
		`${NODE_URL}${definitionPath}`);
	const mosaicInfo = await definitionResponse.json();
	const properties = Object.fromEntries(
		mosaicInfo.properties.map(prop => [prop.name, prop.value]));
	console.log('Mosaic information:');
	console.log('  Creator:', mosaicInfo.creator);
	console.log('  Divisibility:', properties.divisibility);
	console.log('  Initial supply:', properties.initialSupply);
	console.log('  Supply mutable:', properties.supplyMutable);
	console.log('  Transferable:', properties.transferable);
	// [<step-9]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

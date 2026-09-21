import { PrivateKey } from 'symbol-sdk';
import {
	Address,
	NemFacade,
	calculateMosaicRentalFee,
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

const facade = new NemFacade('testnet');
const signerAddress = facade.network.publicKeyToAddress(
	signerKeyPair.publicKey);
console.log('Signer address:', signerAddress.toString());

const namespaceName = process.env.NAMESPACE || 'my_namespace';
const mosaicName = process.env.MOSAIC ||
	`token_${Math.floor(Date.now() / 1000)}`;
const mosaicId = `${namespaceName}:${mosaicName}`;
console.log('Creating mosaic:', mosaicId);
// [<step-1]
try {
	// Describe the levy [>step-2]
	const LEVY_RECIPIENT = process.env.LEVY_RECIPIENT ||
		'TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4';

	const levy = {
		transferFeeType: 'absolute',
		recipientAddress: LEVY_RECIPIENT,
		mosaicId: {
			namespaceId: { name: 'nem' },
			name: 'xem'
		},
		fee: 1_000_000
	};
	console.log('Levy:');
	console.log('  Type:', levy.transferFeeType);
	console.log('  Recipient:', levy.recipientAddress);
	console.log('  Mosaic:',
		`${levy.mosaicId.namespaceId.name}:${levy.mosaicId.name}`);
	console.log('  Fee:', levy.fee);
	// [<step-2]
	// Build the mosaic definition transaction [>step-3]
	const rentalFee = calculateMosaicRentalFee();
	console.log('  Mosaic creation fee:',
		`${Number(rentalFee) / 1_000_000} XEM`);

	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.MosaicDefinitionTransactionV1Descriptor(
			new descriptors.MosaicDefinitionDescriptor(
				signerKeyPair.publicKey,
				new descriptors.MosaicIdDescriptor(
					new descriptors.NamespaceIdDescriptor(namespaceName),
					mosaicName),
				'My tutorial mosaic with a levy',
				[
					['divisibility', '2'],
					['initialSupply', '1000'],
					['supplyMutable', 'true'],
					['transferable', 'true']
				].map(([name, value]) =>
					new descriptors.SizePrefixedMosaicPropertyDescriptor(
						new descriptors.MosaicPropertyDescriptor(
							name, value))),
				new descriptors.MosaicLevyDescriptor(
					models.MosaicTransferFeeType.ABSOLUTE,
					new Address(LEVY_RECIPIENT),
					new descriptors.MosaicIdDescriptor(
						new descriptors.NamespaceIdDescriptor('nem'),
						'xem'),
					new models.Amount(BigInt(levy.fee)))),
			new Address('TBMOSAICOD4F54EE5CDMR23CCBGOAM2XSJBR5OLC'),
			new models.Amount(rentalFee)),
		signerKeyPair.publicKey,
		0n,
		2 * 60 * 60);

	// Calculate and attach the transaction fee
	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log('  Transaction fee:', `${Number(fee) / 1_000_000} XEM`);
	// [<step-3]
	// Sign, announce and wait for confirmation [>step-4]
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built mosaic definition transaction:');
	console.dir(transaction.toJson(), { colors: true });

	const announcePath = '/transaction/announce';
	console.log('Announcing mosaic definition to', announcePath);
	const announceResponse = await fetch(`${NODE_URL}${announcePath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await announceResponse.json();
	console.log('  Result:', announceResult.message);

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
	// [<step-4]
	// Retrieve the levy [>step-5]
	const definitionPath = `/mosaic/definition?mosaicId=${mosaicId}`;
	console.log('Fetching mosaic information from', definitionPath);
	const definitionResponse = await fetch(
		`${NODE_URL}${definitionPath}`);
	const mosaicInfo = await definitionResponse.json();
	const levyInfo = mosaicInfo.levy;
	const levyMosaicId = levyInfo.mosaicId;
	const levyType = 1 === levyInfo.type ? 'absolute' : 'percentile';
	console.log('Levy information:');
	console.log('  Type:', levyType);
	console.log('  Recipient:', levyInfo.recipient);
	console.log('  Mosaic:',
		`${levyMosaicId.namespaceId}:${levyMosaicId.name}`);
	console.log('  Fee:', levyInfo.fee);
	// [<step-5]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

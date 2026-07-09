import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade,
	calculateNamespaceRentalFee,
	calculateTransactionFee,
	models
} from 'symbol-sdk/nem';

const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log('Using node', NODE_URL);

const SIGNER_PRIVATE_KEY = process.env.SIGNER_PRIVATE_KEY ||
	'0000000000000000000000000000000000000000000000000000000000000000';
const signerKeyPair = new NemFacade.KeyPair(
	new PrivateKey(SIGNER_PRIVATE_KEY));

const facade = new NemFacade('testnet');
const signerAddress = facade.network.publicKeyToAddress(
	signerKeyPair.publicKey);
console.log('Signer address:', signerAddress.toString());

try {
	// Fetch current network time
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

	// Build the transaction [>step-1]
	const rootNamespaceName = process.env.ROOT_NAMESPACE || 'ns_root';
	const childNamespaceName = process.env.SUBNAMESPACE ||
		`sub_${Math.floor(Date.now() / 1000)}`;
	const fullNamespaceName =
		`${rootNamespaceName}.${childNamespaceName}`;
	console.log('Creating subnamespace:', fullNamespaceName);

	const rentalFee = calculateNamespaceRentalFee(false);
	console.log('  Namespace lease fee:',
		`${Number(rentalFee) / 1_000_000} XEM`);

	const transaction = facade.transactionFactory.create({
		type: 'namespace_registration_transaction_v1',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		timestamp,
		deadline,
		rentalFeeSink: 'TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35',
		rentalFee,
		parentName: rootNamespaceName,
		name: childNamespaceName
	});

	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	// [<step-1]
	// Sign transaction and generate final payload
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built transaction:');
	console.dir(transaction.toJson(), { colors: true });

	// Announce the transaction
	const announcePath = '/transaction/announce';
	console.log('Announcing namespace registration to', announcePath);
	const announceResponse = await fetch(`${NODE_URL}${announcePath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await announceResponse.json();
	console.log('  Result:', announceResult.message);

	// Wait for confirmation
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

	// Retrieve the namespace [>step-2]
	const namespacePath = `/namespace?namespace=${fullNamespaceName}`;
	console.log('Fetching namespace information from', namespacePath);
	const namespaceResponse = await fetch(`${NODE_URL}${namespacePath}`);
	const namespaceInfo = await namespaceResponse.json();
	console.log('Namespace information:');
	console.log('  Name:', namespaceInfo.fqn);
	console.log('  Owner:', namespaceInfo.owner);
	console.log('  Registration height:', namespaceInfo.height);
	// [<step-2]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

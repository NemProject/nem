import { PrivateKey } from 'symbol-sdk';
import {
	Address,
	NemFacade,
	calculateNamespaceRentalFee,
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
// [<step-1]
try {
	// Build the namespace name [>step-2]
	const namespaceName = process.env.ROOT_NAMESPACE ||
		`ns_${Math.floor(Date.now() / 1000)}`;
	console.log('Creating root namespace:', namespaceName);
	// [<step-2]
	// Build the transaction [>step-3]
	const rentalFee = calculateNamespaceRentalFee(true);
	console.log('  Namespace lease fee:',
		`${Number(rentalFee) / 1_000_000} XEM`);

	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.NamespaceRegistrationTransactionV1Descriptor(
			new Address('TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35'),
			new models.Amount(rentalFee),
			namespaceName),
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
	// [<step-5]
	// Wait for confirmation [>step-6]
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
	// [<step-6]
	// Retrieve the namespace [>step-7]
	const namespacePath = `/namespace?namespace=${namespaceName}`;
	console.log('Fetching namespace information from', namespacePath);
	const namespaceResponse = await fetch(`${NODE_URL}${namespacePath}`);
	const namespaceInfo = await namespaceResponse.json();
	console.log('Namespace information:');
	console.log('  Name:', namespaceInfo.fqn);
	console.log('  Owner:', namespaceInfo.owner);
	console.log('  Registration height:', namespaceInfo.height);
	// [<step-7]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

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

const SIGNER_PRIVATE_KEY = process.env.SIGNER_PRIVATE_KEY ||
	'0000000000000000000000000000000000000000000000000000000000000000';
const signerKeyPair = new NemFacade.KeyPair(
	new PrivateKey(SIGNER_PRIVATE_KEY));

const facade = new NemFacade('testnet');
const signerAddress = facade.network.publicKeyToAddress(
	signerKeyPair.publicKey);
console.log('Signer address:', signerAddress.toString());

try {
	// Choose the subnamespace name [>step-1]
	const rootNamespaceName = process.env.ROOT_NAMESPACE || 'ns_root';
	const childNamespaceName = process.env.SUBNAMESPACE ||
		`sub_${Math.floor(Date.now() / 1000)}`;
	const fullNamespaceName =
		`${rootNamespaceName}.${childNamespaceName}`;
	console.log('Creating subnamespace:', fullNamespaceName);
	// [<step-1]
	// Build the transaction [>step-2]
	const rentalFee = calculateNamespaceRentalFee(false);
	console.log('  Namespace lease fee:',
		`${Number(rentalFee) / 1_000_000} XEM`);

	const transaction = facade.createTransactionFromTypedDescriptor(
		new descriptors.NamespaceRegistrationTransactionV1Descriptor(
			new Address('TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35'),
			new models.Amount(rentalFee),
			childNamespaceName,
			rootNamespaceName),
		signerKeyPair.publicKey,
		0n,
		2 * 60 * 60);

	const fee = calculateTransactionFee(transaction);
	transaction.fee = new models.Amount(fee);
	console.log(`  Transaction fee: ${Number(fee) / 1_000_000} XEM`);
	// [<step-2]
	// Sign transaction and generate final payload
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	console.log('Built transaction:');
	console.dir(transaction.toJson(), { colors: true });
	const transactionHash = facade.hashTransaction(transaction).toString();
	console.log('Transaction hash:', transactionHash);

	// Announce the transaction
	const announceResult = await announceTransaction(
		jsonPayload, 'namespace registration');

	// Wait for confirmation
	if ('SUCCESS' === announceResult)
		await waitForConfirmation(transactionHash, 'namespace registration');
	else
		console.log('Transaction rejected:', announceResult);
	// Retrieve the namespace [>step-3]
	const namespacePath = `/namespace?namespace=${fullNamespaceName}`;
	console.log('Fetching namespace information from', namespacePath);
	const namespaceResponse = await fetch(`${NODE_URL}${namespacePath}`);
	const namespaceInfo = await namespaceResponse.json();
	console.log('Namespace information:');
	console.log('  Name:', namespaceInfo.fqn);
	console.log('  Owner:', namespaceInfo.owner);
	console.log('  Registration height:', namespaceInfo.height);
	// [<step-3]
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
}

const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log('Using node', NODE_URL);

// [>step-1]
// Transaction hash to monitor.
const transactionHash = process.env.TRANSACTION_HASH ||
	'AE0B2142DFB75C9C126442EF612944E926BCE63FA34B353CDE409E2E87703C0B';
// Signer's address.
const signerAddress = process.env.SIGNER_ADDRESS ||
	'TBONKWCOWBZYZB2I5JD3LSDBQVBYHB757VN3SKPP';
// Transaction signature.
const transactionSignature = process.env.TRANSACTION_SIGNATURE ||
	'99B1850FADDB964112D030AA0A5C9F8B5B1B6B992B407D9C70F52F089BD651DF' +
	'A7D4991639A48B810EFD98C45060D7AD9AE57FDA37F58561459DCE8D0A747F02';
// [<step-1]
console.log(`Monitoring transaction: ${transactionHash}`);

// [>step-2]
/**
 * Poll /transaction/get until the transaction is confirmed.
 * @param {string} txHash - hash of the transaction to monitor
 * @param {number} maxAttempts - maximum polling attempts
 * @param {number} waitSeconds - seconds to wait between attempts
 * @returns {boolean} true if the transaction was confirmed
 */
async function waitForTransactionConfirmation(
	txHash,
	maxAttempts = 120,
	waitSeconds = 1
) {
	const statusPath = `/transaction/get?hash=${txHash}`;
	console.log('\nWaiting for transaction confirmation');
	console.log(`Polling ${statusPath}`);

	for (let attempt = 1; attempt <= maxAttempts; attempt++) {
		const response = await fetch(`${NODE_URL}${statusPath}`);
		if (response.ok) {
			const confirmed = await response.json();
			console.log(
				`  Attempt ${attempt}: ` +
				`confirmed in block ${confirmed.meta.height}`
			);
			return true;
		}
		if (400 !== response.status)
			throw new Error(`Unexpected status: ${response.status}`);

		console.log(`  Attempt ${attempt}: pending`);
		// Wait before next attempt (except on last attempt)
		if (attempt < maxAttempts) {
			await new Promise(resolve => {
				setTimeout(resolve, waitSeconds * 1000);
			});
		}
	}
	return false;
} // [<step-2]

// [>step-3]
/**
 * Check whether a transaction with the given signature is in the
 * address's unconfirmed pool.
 * @param {string} signature - hex signature of the monitored transaction
 * @param {string} address - signer's address
 * @returns {boolean} true if the signature is in the signer's pool
 */
async function isInUnconfirmedPool(signature, address) {
	const path = `/account/unconfirmedTransactions?address=${address}`;
	const response = await fetch(`${NODE_URL}${path}`);
	const pool = (await response.json()).data;

	const target = signature.toLowerCase();
	return pool.some(
		entry => entry.transaction.signature.toLowerCase() === target
	);
} // [<step-3]

// [>step-4]
if (await waitForTransactionConfirmation(transactionHash))
	console.log('\nTransaction confirmed!');
else if (await isInUnconfirmedPool(transactionSignature, signerAddress))
	console.log('\nTransaction still in the unconfirmed pool.');
else
	console.log('\nTransaction not in the unconfirmed pool.');

// [<step-4]

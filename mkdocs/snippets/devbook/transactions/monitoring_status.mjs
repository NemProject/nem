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
 * Query /transaction/get once to check for confirmation.
 * @param {string} txHash - hash of the transaction to check
 * @returns {number|null} height of the block containing the
 *   transaction, or null if it is not confirmed yet
 */
async function getConfirmationHeight(txHash) {
	const url = `${NODE_URL}/transaction/get?hash=${txHash}`;
	const response = await fetch(url);
	if (response.ok) {
		const confirmed = await response.json();
		return confirmed.meta.height;
	}
	if (400 !== response.status)
		throw new Error(`Unexpected status: ${response.status}`);
	return null;
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
/**
 * Check for confirmation repeatedly until the transaction is
 * confirmed or the attempts run out.
 * @param {string} txHash - hash of the transaction to monitor
 * @param {number} maxAttempts - maximum polling attempts
 * @param {number} waitSeconds - seconds to wait between attempts
 * @returns {boolean} true if the transaction was confirmed
 */
async function waitForConfirmation(
	txHash,
	maxAttempts = 120,
	waitSeconds = 1
) {
	console.log('\nWaiting for transaction confirmation');
	for (let attempt = 1; attempt <= maxAttempts; attempt++) {
		await new Promise(resolve => {
			setTimeout(resolve, waitSeconds * 1000);
		});
		const height = await getConfirmationHeight(txHash);
		const status = height ? `confirmed in block ${height}` : 'pending';
		console.log(`  Attempt ${attempt}: ${status}`);
		if (height)
			return true;
	}
	return false;
} // [<step-4]

// [>step-5]
try {
	const blockHeight = await getConfirmationHeight(transactionHash);
	if (blockHeight)
		console.log(`\nTransaction already confirmed in block ${blockHeight}`);
	else if (!(await isInUnconfirmedPool(transactionSignature, signerAddress)))
		console.log('\nTransaction not in the unconfirmed pool');
	else if (await waitForConfirmation(transactionHash))
		console.log('\nTransaction confirmed!');
	else
		console.log('\nTransaction not confirmed within the polling window');
} catch (error) {
	console.log(`\nCould not reach the node: ${error.message}`);
}
// [<step-5]

const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log('Using node', NODE_URL);

// [>step-2]
/**
 * Fetch all mosaic balances owned by an account.
 * @param {string} address - Account address
 * @returns {Promise<object[]>} List of mosaics with id and quantity
 */
async function getMosaicBalances(address) {
	const path = `/account/mosaic/owned?address=${address}`;
	const response = await fetch(`${NODE_URL}${path}`);
	const info = await response.json();
	return info.data;
} // [<step-2]
// [>step-3]
/**
 * Fetch mosaic definitions for every mosaic owned by an account.
 * @param {string} address - Account address
 * @returns {Promise<Map>} Map of "namespace:name" to mosaic definition
 */
async function getMosaicDefinitions(address) {
	const path = `/account/mosaic/owned/definition?address=${address}`;
	const response = await fetch(`${NODE_URL}${path}`);
	const info = await response.json();
	// Build a map from "namespace:name" to mosaic definition
	const definitionsMap = new Map();
	for (const entry of info.data) {
		const key = `${entry.id.namespaceId}:${entry.id.name}`;
		definitionsMap.set(key, entry);
	}
	return definitionsMap;
} // [<step-3]
// [>step-4]
/**
 * Format an atomic amount with decimal places.
 * @param {bigint} amount - The atomic amount
 * @param {number} divisibility - Number of decimal places
 * @returns {string} The formatted amount
 */
function formatAmount(amount, divisibility) {
	if (0 === divisibility)
		return amount.toString();

	const divisor = 10n ** BigInt(divisibility);
	const wholePart = amount / divisor;
	const fractionalPart = amount % divisor;
	const fractionalStr = fractionalPart.toString()
		.padStart(divisibility, '0');
	return `${wholePart}.${fractionalStr}`;
}
// [<step-4]
// The account address to query [>step-5]
const ADDRESS = process.env.ADDRESS ||
	'TBONKWCOWBZYZB2I5JD3LSDBQVBYHB757VN3SKPP';
console.log('Fetching balances for', ADDRESS);

try {
	// Fetch mosaic balances and definitions for the account
	const accountMosaics = await getMosaicBalances(ADDRESS);
	const mosaicDefinitions = await getMosaicDefinitions(ADDRESS);

	if (0 === accountMosaics.length) {
		console.log('Account holds no mosaics');
	} else {
		console.log(`Account holds ${accountMosaics.length} mosaic(s):`);

		for (const mosaicEntry of accountMosaics) {
			const { mosaicId } = mosaicEntry;
			const key = `${mosaicId.namespaceId}:${mosaicId.name}`;
			const balance = BigInt(mosaicEntry.quantity);

			// Get mosaic divisibility from the definition
			const definition = mosaicDefinitions.get(key);
			const properties = Object.fromEntries(
				definition.properties.map(p => [p.name, p.value])
			);
			const divisibility = parseInt(
				properties.divisibility || '0', 10);

			// Format and display the balance
			const formattedBalance = formatAmount(balance, divisibility);
			console.log(`- Mosaic ${key}`);
			console.log(`  Balance: ${formattedBalance}`);
			console.log(`  Balance (atomic): ${balance.toString()}`);
			console.log(`  Divisibility: ${divisibility}`);
		}
	}
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
} // [<step-5]

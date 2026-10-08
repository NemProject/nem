const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log('Using node', NODE_URL);

const MOSAIC_ID = process.env.MOSAIC_ID || 'nem:xem';
console.log('Mosaic ID:', MOSAIC_ID);

try {
	// Fetch mosaic information [>step-1]
	const mosaicPath = `/mosaic/definition?mosaicId=${MOSAIC_ID}`;
	console.log('Fetching mosaic information from', mosaicPath);
	const mosaicResponse = await fetch(`${NODE_URL}${mosaicPath}`);
	if (!mosaicResponse.ok)
		throw new Error(`HTTP error! status: ${mosaicResponse.status}`);

	const mosaicJSON = await mosaicResponse.json();
	const fullName = `${mosaicJSON.id.namespaceId}:${mosaicJSON.id.name}`;
	console.log('Mosaic information:');
	console.log(`  Mosaic ID: ${fullName}`);
	console.log('  Description:', mosaicJSON.description);
	console.log('  Creator:', mosaicJSON.creator);
	const properties = Object.fromEntries(mosaicJSON.properties
		.map(property => [property.name, property.value]));
	const divisibility = parseInt(properties.divisibility, 10);
	console.log('  Divisibility:', divisibility);
	console.log('  Initial supply:', properties.initialSupply);
	console.log('  Supply mutable:', properties.supplyMutable);
	console.log('  Transferable:', properties.transferable);
	const hasLevy = 0 !== Object.keys(mosaicJSON.levy).length;
	console.log('  Levy:', hasLevy ? mosaicJSON.levy : 'none');
	// [<step-1]
	// Fetch the current supply [>step-2]
	const supplyPath = `/mosaic/supply?mosaicId=${MOSAIC_ID}`;
	console.log('\nFetching current supply from', supplyPath);
	const supplyResponse = await fetch(`${NODE_URL}${supplyPath}`);
	const supplyInfo = await supplyResponse.json();
	const supply = supplyInfo.supply;
	console.log('  Current supply:', supply);
	// [<step-2]
	// Convert the supply to atomic units [>step-3]
	const atomic = BigInt(supply) * (10n ** BigInt(divisibility));
	console.log(`\nSupply in atomic units: ${atomic}`);
	// [<step-3]
} catch (e) {
	console.error(e.message);
}

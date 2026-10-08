import {
	NemFacade,
	NetworkTimestamp
} from 'symbol-sdk/nem';
// [>step-1]
const facade = new NemFacade('testnet');
console.log(`Network name: ${facade.network.name}`);
// NetworkTimestamp(0) is the genesis block timestamp (network launch)
const launchDate = facade.network.toDatetime(new NetworkTimestamp(0));
console.log(`Network launch date: ${launchDate.toISOString()}`); // [<step-1]
// [>step-2]
const NODE_URL = 'http://libertalia.nemtest.net:7890';
console.log(`Using node ${NODE_URL}`);
try {
	// Fetch current chain height
	const heightPath = '/chain/height';
	console.log(`Fetching chain height from ${heightPath}`);
	const response = await fetch(`${NODE_URL}${heightPath}`,
		{ timeout: 10000 });
	if (!response.ok)
		throw new Error(`HTTP error! status: ${response.status}`);
	const responseJson = await response.json();
	const height = parseInt(responseJson.height, 10);
	console.log(`  Blockchain height: ${height.toLocaleString()} blocks`);
} catch (e) {
	console.error(e.message, '| Cause:', e.cause?.code ?? 'unknown');
} // [<step-2]

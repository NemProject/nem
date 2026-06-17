import { PublicKey } from 'symbol-sdk';
import { NemFacade } from 'symbol-sdk/nem';

const fmt = v => (Number(v) / 1e6).toLocaleString(
	'en-US', { minimumFractionDigits: 6 });

const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log(`Using node ${NODE_URL}`);

const BLOCK_HEIGHT = process.env.BLOCK_HEIGHT || '661258';

const facade = new NemFacade('testnet');

try {
	// Fetch the block at the given height [>step-1]
	const blockUrl = `${NODE_URL}/block/at/public`;
	const response = await fetch(blockUrl, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ height: parseInt(BLOCK_HEIGHT, 10) })
	});
	if (!response.ok)
		throw new Error(`HTTP error! status: ${response.status}`);
	const block = await response.json();
	const transactions = block.transactions;
	console.log(`Block height: ${BLOCK_HEIGHT}`);
	console.log(`Transactions: ${transactions.length}`);
	// [<step-1]
	// Identify the harvester [>step-2]
	const harvester = facade.network.publicKeyToAddress(
		new PublicKey(block.signer));
	console.log(`Harvester: ${harvester}`);
	// [<step-2]
	// Sum the transaction fees [>step-3]
	let reward = 0n;
	console.log('\nTransaction fees:');
	for (const transaction of transactions) {
		const fee = BigInt(transaction.fee);
		reward += fee;
		console.log(`  Fee: ${fmt(fee)} XEM`);
	}
	// [<step-3]
	// Total reward [>step-4]
	console.log(`\nTotal block reward: ${fmt(reward)} XEM`);
	// [<step-4]
} catch (error) {
	console.log(error);
}

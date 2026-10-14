const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log(`Using node ${NODE_URL}`);

let prevHeight = null;
let heightChangedAt = null;

const REWRITE_LIMIT = 360;

for (;;) {
	const response = await fetch(`${NODE_URL}/chain/height`); // [>step-1]
	if (!response.ok)
		throw new Error(`HTTP error! status: ${response.status}`);

	const chainHeight = await response.json();

	const height = parseInt(chainHeight.height, 10); // [<step-1]
	// [>step-2]
	const irreversibleHeight = Math.max(0, height - REWRITE_LIMIT);
	// [<step-2]
	const now = Date.now(); // [>step-3]
	if (null !== prevHeight && height !== prevHeight)
		heightChangedAt = now;

	const heightAgo = null !== heightChangedAt ?
		`${Math.floor((now - heightChangedAt) / 1000)}s ago` :
		'-'; // [<step-3]
	// [>step-4]
	const heightLabel = height.toLocaleString().padStart(10);
	const irreversibleLabel =
		irreversibleHeight.toLocaleString().padStart(10);
	console.log(
		`Height: ${heightLabel}  (changed ${heightAgo})` +
		`  |  Irreversible: ${irreversibleLabel}`
	);

	prevHeight = height;
	await new Promise(resolve => { setTimeout(resolve, 1000); });
	// [<step-4]
}

const NODE_URL = process.env.NODE_URL ||
	'http://portobelo.nemmain.net:7890';
console.log(`Using node ${NODE_URL}`);

try {
	const fmt = xem =>
		xem.toLocaleString('en-US', { minimumFractionDigits: 6 });

	const MOSAIC_ID = 'nem:xem'; // [>step-1]
	const supplyPath = `/mosaic/supply?mosaicId=${MOSAIC_ID}`;
	const response = await fetch(`${NODE_URL}${supplyPath}`);
	const supplyInfo = await response.json();
	const totalSupply = supplyInfo.supply;
	console.log(`Total supply: ${fmt(totalSupply)} ${MOSAIC_ID}`); // [<step-1]
	// Read the mosaic's divisibility to convert balances to whole units [>step-2]
	const definitionPath = `/mosaic/definition?mosaicId=${MOSAIC_ID}`;
	const definitionResponse =
		await fetch(`${NODE_URL}${definitionPath}`);
	const definition = await definitionResponse.json();
	const properties = Object.fromEntries(
		definition.properties.map(
			property => [property.name, property.value]));
	const divisibility = parseInt(properties.divisibility, 10);
	// [<step-2]
	// [>step-3]
	const scale = 10n ** BigInt(divisibility);

	const fmtAtomic = atomic =>
		`${(atomic / scale).toLocaleString('en-US')}.` +
		`${(atomic % scale).toString().padStart(divisibility, '0')}`;

	const NON_CIRCULATING_ADDRESSES = [
		['Treasury', 'NCHESTYVD2P6P646AMY7WSNG73PCPZDUQNSD6JAK'],
		['Nemesis', 'NANEMOABLAGR72AZ2RV3V4ZHDCXW25XQ73O7OBT5'],
		['Namespace rental', 'NAMESPACEWH4MKFMBCVFERDPOOP4FK7MTBXDPZZA'],
		['Mosaic rental', 'NBMOSAICOD4F54EE5CDMR23CCBGOAM2XSIUX6TRS']
	];
	let nonCirculatingSupply = 0n;
	for (const [label, address] of NON_CIRCULATING_ADDRESSES) {
		const accountPath = `/account/get?address=${address}`;
		const accountResponse = await fetch(`${NODE_URL}${accountPath}`);
		const accountInfo = await accountResponse.json();
		const balance = BigInt(accountInfo.account.balance);
		nonCirculatingSupply += balance;
		console.log(`  ${label}: ${fmtAtomic(balance)} ${MOSAIC_ID}`);
	}
	console.log(
		'Non-circulating supply: ' +
		`${fmtAtomic(nonCirculatingSupply)} ${MOSAIC_ID}`
	); // [<step-3]
	// [>step-4]
	const circulatingSupply =
		(BigInt(totalSupply) * scale) - nonCirculatingSupply;
	console.log(
		'Circulating supply: ' +
		`${fmtAtomic(circulatingSupply)} ${MOSAIC_ID}`
	); // [<step-4]
} catch (error) {
	console.log(error);
}

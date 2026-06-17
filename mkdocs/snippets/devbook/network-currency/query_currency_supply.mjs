const NODE_URL = process.env.NODE_URL ||
	'http://portobelo.nemmain.net:7890';
console.log(`Using node ${NODE_URL}`);

try {
	const fmt = xem => 	// [>step-1]
		xem.toLocaleString('en-US', { minimumFractionDigits: 6 });

	const supplyPath = '/mosaic/supply?mosaicId=nem:xem';
	const response = await fetch(`${NODE_URL}${supplyPath}`);
	const supplyInfo = await response.json();
	const totalSupply = supplyInfo.supply;
	console.log(`Total supply: ${fmt(totalSupply)} XEM`); // [<step-1]
	// [>step-2]
	const NON_CIRCULATING_ADDRESSES = [
		['Treasury', 'NCHESTYVD2P6P646AMY7WSNG73PCPZDUQNSD6JAK'],
		['Nemesis', 'NANEMOABLAGR72AZ2RV3V4ZHDCXW25XQ73O7OBT5'],
		['Namespace rental', 'NAMESPACEWH4MKFMBCVFERDPOOP4FK7MTBXDPZZA'],
		['Mosaic rental', 'NBMOSAICOD4F54EE5CDMR23CCBGOAM2XSIUX6TRS']
	];
	let nonCirculatingSupply = 0;
	for (const [label, address] of NON_CIRCULATING_ADDRESSES) {
		const accountPath = `/account/get?address=${address}`;
		const accountResponse = await fetch(`${NODE_URL}${accountPath}`);
		const accountInfo = await accountResponse.json();
		const balance = accountInfo.account.balance / 1_000_000;
		nonCirculatingSupply += balance;
		console.log(`  ${label}: ${fmt(balance)} XEM`);
	}
	console.log(
		`Non-circulating supply: ${fmt(nonCirculatingSupply)} XEM`
	); // [<step-2]
	// [>step-3]
	const circulatingSupply = totalSupply - nonCirculatingSupply;
	console.log(`Circulating supply: ${fmt(circulatingSupply)} XEM`); // [<step-3]
} catch (error) {
	console.log(error);
}

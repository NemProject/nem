const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
console.log('Using node', NODE_URL);

const NAMESPACE_NAME = process.env.NAMESPACE_NAME || 'company';
console.log('Namespace name:', NAMESPACE_NAME);

try {
	// Fetch namespace information [>step-1]
	const namespacePath = `/namespace?namespace=${NAMESPACE_NAME}`;
	console.log('Fetching namespace information from', namespacePath);
	const namespaceResponse = await fetch(`${NODE_URL}${namespacePath}`);
	if (!namespaceResponse.ok)
		throw new Error(`HTTP error! status: ${namespaceResponse.status}`);

	const namespaceInfo = await namespaceResponse.json();
	console.log('Namespace information:');
	console.log('  Name:', namespaceInfo.fqn);
	console.log('  Owner:', namespaceInfo.owner);
	const leaseHeight = namespaceInfo.height;
	console.log('  Height:', leaseHeight);
	// [<step-1]
	// Compute the lease expiration [>step-2]
	const LEASE_DURATION = 525600; // approximately one year of blocks
	const chainResponse = await fetch(`${NODE_URL}/chain/height`);
	const currentHeight = (await chainResponse.json()).height;
	const expirationHeight = leaseHeight + LEASE_DURATION;
	console.log('\nCurrent chain height:', currentHeight);
	console.log('Lease expiration height:', expirationHeight);
	console.log('Blocks until expiration:',
		expirationHeight - currentHeight);
	// [<step-2]
	// List the subnamespaces [>step-3]
	const owner = namespaceInfo.owner;
	const subnamespacesPath = '/account/namespace/page' +
		`?address=${owner}&parent=${NAMESPACE_NAME}`;
	console.log('\nFetching subnamespaces from', subnamespacesPath);
	const subnamespacesResponse =
		await fetch(`${NODE_URL}${subnamespacesPath}`);
	const subnamespaces = (await subnamespacesResponse.json()).data;
	console.log(`Subnamespaces of ${NAMESPACE_NAME}:`,
		subnamespaces.length);
	for (const subnamespace of subnamespaces)
		console.log(`  ${subnamespace.fqn}`);
	// [<step-3]
	// List the mosaics defined under the namespace [>step-4]
	const mosaicsPath =
		`/namespace/mosaic/definition/page?namespace=${NAMESPACE_NAME}`;
	console.log('\nFetching mosaic definitions from', mosaicsPath);
	const mosaicsResponse = await fetch(`${NODE_URL}${mosaicsPath}`);
	const mosaics = (await mosaicsResponse.json()).data;
	console.log(`Mosaics defined under ${NAMESPACE_NAME}:`,
		mosaics.length);
	for (const entry of mosaics) {
		const mosaicId = entry.mosaic.id;
		console.log(`  ${mosaicId.namespaceId}:${mosaicId.name}`);
	}
	// [<step-4]
} catch (e) {
	console.error(e.message);
}

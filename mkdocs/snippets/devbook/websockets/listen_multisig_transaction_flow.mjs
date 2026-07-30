import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { PrivateKey, PublicKey } from 'symbol-sdk';
import {
	NemFacade, NetworkTimestamp, calculateTransactionFee, models
} from 'symbol-sdk/nem';

const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
const WS_URL = NODE_URL.replace(':7890', ':7778');
console.log(`Using node ${NODE_URL}`);

const facade = new NemFacade('testnet');
// Set up the multisig and cosignatory accounts [>step-1]
const MULTISIG_PUBLIC_KEY = process.env.MULTISIG_PUBLIC_KEY || (
	'D656155B48D4E71E4C59EC6FAEB5EB4F214DE8BC3C65D5BF6A3D9931B4E5ACF2');
const multisigPublicKey = new PublicKey(MULTISIG_PUBLIC_KEY);
const multisigAddress = facade.network.publicKeyToAddress(
	multisigPublicKey).toString();
console.log(`Multisig address: ${multisigAddress}`);
const COSIGNATORY0_PRIVATE_KEY = process.env.COSIGNATORY0_PRIVATE_KEY || (
	'0000000000000000000000000000000000000000000000000000000000000002');
const cosignatory0KeyPair = new NemFacade.KeyPair(
	new PrivateKey(COSIGNATORY0_PRIVATE_KEY));
console.log(`Cosignatory 0 public key: ${cosignatory0KeyPair.publicKey}`);
const COSIGNATORY1_PRIVATE_KEY = process.env.COSIGNATORY1_PRIVATE_KEY || (
	'0000000000000000000000000000000000000000000000000000000000000003');
const cosignatory1KeyPair = new NemFacade.KeyPair(
	new PrivateKey(COSIGNATORY1_PRIVATE_KEY));
console.log(`Cosignatory 1 public key: ${cosignatory1KeyPair.publicKey}`);
// [<step-1]

try {
	// [Cosignatory 0] Build and sign the multisig transaction [>step-2]
	const timeResponse = await fetch(
		`${NODE_URL}/time-sync/network-time`);
	const networkTime = Math.floor(
		(await timeResponse.json()).receiveTimeStamp / 1000);
	const timestamp = new NetworkTimestamp(networkTime);
	const deadline = timestamp.addHours(2);

	const transferTransaction = facade.transactionFactory.create({
		type: 'transfer_transaction_v2',
		signerPublicKey: multisigPublicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		recipientAddress: multisigAddress,
		amount: 1_000_000n // 1 XEM
	});
	transferTransaction.fee = new models.Amount(
		calculateTransactionFee(transferTransaction));

	const transaction = facade.transactionFactory.create({
		type: 'multisig_transaction_v1',
		signerPublicKey: cosignatory0KeyPair.publicKey.toString(),
		timestamp: timestamp.timestamp,
		deadline: deadline.timestamp,
		innerTransaction: facade.transactionFactory.static
			.toNonVerifiableTransaction(transferTransaction)
	});
	transaction.fee = new models.Amount(
		calculateTransactionFee(transaction));

	const signature = facade.signTransaction(
		cosignatory0KeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	const transactionHash =
		facade.hashTransaction(transaction).toString().toUpperCase();
	const shortHash = transactionHash.substring(0, 16);
	console.log(
		`[Cosignatory 0] Built multisig transaction ${shortHash}...`);
	// [<step-2]
	// [Cosignatory 1] Connect to the WebSocket [>step-3]
	const client = new Client({
		webSocketFactory: () => new SockJS(`${WS_URL}/w/messages`)
	});
	await new Promise(resolve => {
		client.onConnect = resolve;
		client.activate();
	});
	console.log(`[Cosignatory 1] Connected to ${WS_URL}`);
	// [<step-3]
	// [Cosignatory 1] Select the pending multisig transaction [>step-7]
	let innerTransactionHash = null;
	let resolveCosigned;
	const cosigned = new Promise(resolve => {
		resolveCosigned = resolve;
	});
	const onUnconfirmed = async message => {
		if (null !== innerTransactionHash)
			return;
		const body = JSON.parse(message.body);
		const signer = (body.transaction.otherTrans?.signer ?? '')
			.toUpperCase();
		if (multisigPublicKey.toString() !== signer)
			return;
		innerTransactionHash = body.meta.innerHash.data;
		console.log(
			'unconfirmed: innerHash=' +
			`${innerTransactionHash.substring(0, 16)}...`);
		// [<step-7]
		// [Cosignatory 1] Cosign the pending transaction [>step-8]
		const cosignature = facade.transactionFactory.create({
			type: 'cosignature_v1',
			// This is the cosignatory providing the second signature
			signerPublicKey: cosignatory1KeyPair.publicKey.toString(),
			timestamp: timestamp.timestamp,
			deadline: deadline.timestamp,
			// Hash of the inner transfer transaction
			otherTransactionHash: innerTransactionHash,
			// Address of the multisig account
			multisigAccountAddress: multisigAddress
		});
		cosignature.fee = new models.Amount(
			calculateTransactionFee(cosignature));
		const cosignatureSignature = facade.signTransaction(
			cosignatory1KeyPair, cosignature);
		const cosignaturePayload = facade.transactionFactory.static
			.attachSignature(cosignature, cosignatureSignature);
		const cosignatureResponse = await fetch(
			`${NODE_URL}/transaction/announce`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: cosignaturePayload
			});
		const cosignatureResult = await cosignatureResponse.json();
		if ('SUCCESS' !== cosignatureResult.message) {
			console.log(
				`Cosignature rejected: ${cosignatureResult.message}`);
			resolveCosigned(false);
			return;
		}
		console.log('[Cosignatory 1] Announced cosignature');
		resolveCosigned(true);
		// [<step-8]
	};
	// [Cosignatory 1] Wait for confirmation [>step-9]
	let confirmed = false;
	let resolveRegistered;
	let resolveDone;
	const registered = new Promise(resolve => {
		resolveRegistered = resolve;
	});
	const done = new Promise(resolve => {
		resolveDone = resolve;
	});
	const onConfirmed = message => {
		const messageHash = JSON.parse(message.body).meta.innerHash.data;
		console.log(
			`confirmed: innerHash=${messageHash.substring(0, 16)}...`);
		if (messageHash === innerTransactionHash && !confirmed) {
			console.log('Multisig transaction confirmed');
			confirmed = true;
		}
	};
	const onAccountUpdate = message => {
		const { balance } = JSON.parse(message.body).account;
		console.log(`Account update: balance=${balance}`);
		resolveRegistered();
		if (confirmed)
			resolveDone();
	};
	// [<step-9]
	// [Cosignatory 1] Subscribe to the multisig account channels [>step-4]
	const accountChannel = `/account/${multisigAddress}`;
	const subscriptions = [
		{ channel: accountChannel, handler: onAccountUpdate, id: 'id-0' },
		{
			channel: `/unconfirmed/${multisigAddress}`,
			handler: onUnconfirmed,
			id: 'id-1'
		},
		{
			channel: `/transactions/${multisigAddress}`,
			handler: onConfirmed,
			id: 'id-2'
		}
	];
	for (const { channel, handler, id } of subscriptions) {
		client.subscribe(channel, handler, { id });
		console.log(`[Cosignatory 1] Subscribed to ${channel} channel`);
	}
	// [<step-4]
	// [Cosignatory 1] Register the multisig account [>step-5]
	client.publish({
		destination: '/w/api/account/get',
		body: JSON.stringify({ account: multisigAddress })
	});
	await registered;
	console.log('[Cosignatory 1] Multisig account registered');
	// [<step-5]
	// [Cosignatory 0] Announce the multisig transaction [>step-6]
	console.log(
		'[Cosignatory 0] Announcing multisig transaction ' +
		`${shortHash}...`);
	const response = await fetch(`${NODE_URL}/transaction/announce`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await response.json();
	if ('SUCCESS' === announceResult.message) {
		// The transaction is now waiting for the second signature
		// [<step-6]
		// Wait for the cosignature to be announced and the
		// transaction to confirm
		if (await cosigned)
			await done;
	} else {
		console.log(`Transaction rejected: ${announceResult.message}`);
	}
	// [Cosignatory 1] Unsubscribe before closing [>step-10]
	for (const { id } of subscriptions)
		client.unsubscribe(id);
	console.log('[Cosignatory 1] Unsubscribed from all channels');
	client.deactivate(); // [<step-10]
} catch (error) {
	console.error(error);
}

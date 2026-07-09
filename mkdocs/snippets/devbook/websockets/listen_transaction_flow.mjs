import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade, calculateTransactionFee, models
} from 'symbol-sdk/nem';

const NODE_URL = process.env.NODE_URL ||
	'http://libertalia.nemtest.net:7890';
const WS_URL = NODE_URL.replace(':7890', ':7778');
console.log(`Using node ${NODE_URL}`);
// Set up the monitored address and signer [>step-1]
const MONITOR_ADDRESS = process.env.MONITOR_ADDRESS ||
	'TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4';
console.log(`Monitoring address: ${MONITOR_ADDRESS}`);

const SIGNER_PRIVATE_KEY = process.env.SIGNER_PRIVATE_KEY ||
	'0000000000000000000000000000000000000000000000000000000000000000';
const facade = new NemFacade('testnet');
const signerKeyPair = new NemFacade.KeyPair(
	new PrivateKey(SIGNER_PRIVATE_KEY)); // [<step-1]

try {
	// Build and sign a transfer to the monitored address [>step-2]
	const timeResponse = await fetch(
		`${NODE_URL}/time-sync/network-time`);
	const networkTime = Math.floor(
		(await timeResponse.json()).receiveTimeStamp / 1000);
	const transaction = facade.transactionFactory.create({
		type: 'transfer_transaction_v2',
		signerPublicKey: signerKeyPair.publicKey.toString(),
		timestamp: networkTime,
		deadline: networkTime + (2 * 60 * 60),
		recipientAddress: MONITOR_ADDRESS,
		amount: 0n
	});
	transaction.fee = new models.Amount(
		calculateTransactionFee(transaction));
	const signature = facade.signTransaction(signerKeyPair, transaction);
	const jsonPayload = facade.transactionFactory.static.attachSignature(
		transaction, signature);
	const transactionHash =
		facade.hashTransaction(transaction).toString().toUpperCase();
	const shortHash = transactionHash.substring(0, 16);
	// [<step-2]
	// Connect to the WebSocket [>step-3]
	const client = new Client({
		webSocketFactory: () => new SockJS(`${WS_URL}/w/messages`)
	});
	await new Promise(resolve => {
		client.onConnect = resolve;
		client.activate();
	});
	console.log(`Connected to ${WS_URL}`);
	// [<step-3]
	// Subscribe to the account and transaction channels [>step-4]
	const accountChannel = `/account/${MONITOR_ADDRESS}`;
	const channels = {
		[accountChannel]: 'id-0',
		[`/unconfirmed/${MONITOR_ADDRESS}`]: 'id-1',
		[`/transactions/${MONITOR_ADDRESS}`]: 'id-2'
	};
	let confirmed = false;
	let resolveRegistered;
	let resolveDone;
	const registered = new Promise(resolve => {
		resolveRegistered = resolve;
	});
	const done = new Promise(resolve => {
		resolveDone = resolve;
	});
	const onMessage = message => {
		const body = JSON.parse(message.body);
		const { destination } = message.headers;
		if (accountChannel === destination) {
			const { balance } = body.account;
			console.log(`Account update: balance=${balance}`);
			resolveRegistered();
			if (confirmed)
				resolveDone();
		} else if (destination.includes('/transactions/')) {
			const messageHash = body.meta.hash.data;
			console.log(
				`confirmed: hash=${messageHash.substring(0, 16)}...`);
			if (messageHash.toUpperCase() === transactionHash) {
				console.log(`Transaction ${shortHash}... confirmed`);
				confirmed = true;
			}
		} else {
			const messageHash = body.meta.hash.data;
			if (messageHash.toUpperCase() === transactionHash) {
				console.log(
					'unconfirmed: hash=' +
					`${messageHash.substring(0, 16)}...`);
			}
		}
	};
	for (const [channel, id] of Object.entries(channels)) {
		client.subscribe(channel, onMessage, { id });
		console.log(`Subscribed to ${channel} channel`);
	}
	// [<step-4]
	// Register the account and confirm it is active [>step-5]
	client.publish({
		destination: '/w/api/account/get',
		body: JSON.stringify({ account: MONITOR_ADDRESS })
	});
	await registered;
	console.log('Account registered');
	// [<step-5]
	// Announce the transaction and wait for it to confirm [>step-6]
	console.log(`Announcing transaction ${shortHash}...`);
	const response = await fetch(`${NODE_URL}/transaction/announce`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await response.json();
	if ('SUCCESS' === announceResult.message)
		await done;
	else
		console.log(`Transaction rejected: ${announceResult.message}`);
	// [<step-6]
	// Unsubscribe before closing [>step-7]
	for (const id of Object.values(channels))
		client.unsubscribe(id);
	console.log('Unsubscribed from all channels');
	client.deactivate(); // [<step-7]
} catch (error) {
	console.error(error);
}

import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { PrivateKey } from 'symbol-sdk';
import {
	NemFacade, calculateTransactionFee, models
} from 'symbol-sdk/nem';

const NODE_HOST = process.env.NODE_HOST || 'libertalia.nemtest.net';
const NODE_URL = `http://${NODE_HOST}:7890`;
const WS_URL = `http://${NODE_HOST}:7778`;
console.log(`Using node ${NODE_HOST}`);
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
	// Connect to the WebSocket [>step-2]
	const client = new Client({
		webSocketFactory: () => new SockJS(`${WS_URL}/w/messages`)
	});
	await new Promise(resolve => {
		client.onConnect = resolve;
		client.activate();
	});
	console.log(`Connected to ${WS_URL}`);
	// [<step-2]
	// Register the account and confirm it is active [>step-3]
	const account = `/account/${MONITOR_ADDRESS}`;
	await new Promise(resolve => {
		client.subscribe(account, () => {
			client.unsubscribe('id-0');
			resolve();
		}, { id: 'id-0' });
		client.publish({
			destination: '/w/api/account/get',
			body: JSON.stringify({ account: MONITOR_ADDRESS })
		});
	});
	console.log('Account registered');
	// [<step-3]
	// Subscribe to the transaction channels [>step-4]
	const channels = {
		[`/unconfirmed/${MONITOR_ADDRESS}`]: 'id-1',
		[`/transactions/${MONITOR_ADDRESS}`]: 'id-2'
	};
	// The message handler is set later, when waiting for confirmation
	let handleMessage;
	const onMessage = message => handleMessage?.(message);
	for (const [destination, id] of Object.entries(channels)) {
		client.subscribe(destination, onMessage, { id });
		console.log(`Subscribed to ${destination} channel`);
	}
	// [<step-4]
	// Build and sign a transfer to the monitored address [>step-5]
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
		facade.hashTransaction(transaction).toString();
	// [<step-5]
	// Announce the transaction and wait for it to confirm [>step-6]
	const shortHash = transactionHash.substring(0, 16);
	const expectedHash = transactionHash.toUpperCase();
	const confirmed = new Promise(resolve => {
		handleMessage = message => {
			const pair = JSON.parse(message.body);
			const messageHash = pair.meta.hash.data;
			const messageShort = messageHash.substring(0, 16);
			const name = message.headers.destination
				.includes('/transactions/') ? 'confirmed' : 'unconfirmed';
			console.log(`${name}: hash=${messageShort}...`);
			if ('confirmed' === name &&
				messageHash.toUpperCase() === expectedHash) {
				console.log(`Transaction ${shortHash}... confirmed`);
				resolve();
			}
		};
	});
	console.log(`Announcing transaction ${shortHash}...`);
	const response = await fetch(`${NODE_URL}/transaction/announce`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: jsonPayload
	});
	const announceResult = await response.json();
	if ('SUCCESS' === announceResult.message)
		await confirmed;
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

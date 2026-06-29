import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const NODE_HOST = process.env.NODE_HOST || 'libertalia.nemtest.net';
const WS_URL = `http://${NODE_HOST}:7778`;
console.log(`Using node ${NODE_HOST}`);

// Open connection [>step-1]
const client = new Client({
	webSocketFactory: () => new SockJS(`${WS_URL}/w/messages`)
});
await new Promise(resolve => {
	client.onConnect = resolve;
	client.activate();
});
console.log(`Connected to ${WS_URL}`);
// [<step-1]
// Read and format each new block [>step-3]
function formatBlock(message) {
	const block = JSON.parse(message.body);
	console.log(
		`New block: height=${block.height.toLocaleString()}` +
		` harvester=${block.signer.substring(0, 16)}...`
	);
}
// [<step-3]
// Subscribe to the new block channel [>step-2]
const subscription = client.subscribe('/blocks', formatBlock, {
	id: 'id-0'
});
console.log('Subscribed to /blocks channel');
// [<step-2]
// Unsubscribe on exit [>step-4]
process.on('SIGINT', () => {
	subscription.unsubscribe();
	client.deactivate();
	console.log('Unsubscribed and disconnected');
	process.exit(0);
});
// [<step-4]

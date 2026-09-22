//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3
//DEPS org.glassfish.tyrus.bundles:tyrus-standalone-client:2.2.0

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.Address;
import org.symbol.sdk.nem.FeeCalculator;
import org.symbol.sdk.nem.KeyPair;
import org.symbol.sdk.nem.NemTransactionFactory;
import org.symbol.sdk.nem.descriptors.TransferTransactionV2Descriptor;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.Transaction;

@ClientEndpoint
public final class ListenTransactionFlow {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private final BlockingQueue<String> rawFrames =
		new LinkedBlockingQueue<>();

	private final String nodeUrl = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	private final String wsUrl = nodeUrl.replace(":7890", ":7778");

	private final NemFacade facade = new NemFacade("testnet");

	private String announceTransaction(
		final String payload,
		final String endpoint,
		final String label
	) throws IOException, InterruptedException {
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(nodeUrl + endpoint))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(payload))
			.build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		if (2 != response.statusCode() / 100)
			throw new IOException("HTTP " + response.statusCode());
		final String result = JSON_MAPPER.readTree(response.body())
			.get("message").asText();
		if ("SUCCESS".equals(result))
			System.out.println(label);
		return result;
	}

	public static void main(final String[] args) {
		try {
			new ListenTransactionFlow().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws Exception {
		System.out.printf("Using node %s%n", nodeUrl);

		// Set up the monitored address and signer [>step-1]
		final String monitorAddress = System.getenv().getOrDefault(
			"MONITOR_ADDRESS",
			"TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4");
		System.out.printf("Monitoring address: %s%n", monitorAddress);
		final String privateKey = System.getenv().getOrDefault(
			"SIGNER_PRIVATE_KEY", "0".repeat(64));
		final KeyPair signerKeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(privateKey));
		// [<step-1]
		// Build and sign a transfer to the monitored address [>step-2]
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new TransferTransactionV2Descriptor(
					new Address(monitorAddress), new Amount(0),
					null, List.of()),
				signerKeyPair.getPublicKey(), 0, 2 * 60 * 60);
		transaction.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(transaction)));
		final String payload = NemTransactionFactory.attachSignature(
			transaction,
			facade.signTransaction(signerKeyPair, transaction));
		final String transactionHash = facade.hashTransaction(transaction)
			.toString().toUpperCase();
		// [<step-2]
		// Connect to the WebSocket [>step-3]
		final Session session = ContainerProvider.getWebSocketContainer()
			.connectToServer(this,
				URI.create(sockJsUrl(wsUrl + "/w/messages")));
		final RemoteEndpoint.Basic remote = session.getBasicRemote();
		rawFrames.take(); // consume the SockJS open frame
		sendFrame(remote, "CONNECT\naccept-version:1.1,1.0\n"
			+ "heart-beat:0,0\nhost:" + nodeUrl + "\n\n\0");
		System.out.printf("Connected to %s%n", wsUrl);
		// [<step-3]
		// Subscribe to the account and transaction channels [>step-4]
		final String accountChannel = "/account/" + monitorAddress;
		final Map<String, String> channels = new HashMap<>();
		channels.put(accountChannel, "id-0");
		channels.put("/unconfirmed/" + monitorAddress, "id-1");
		channels.put("/transactions/" + monitorAddress, "id-2");
		for (final Map.Entry<String, String> channel
			: channels.entrySet()) {
			subscribe(remote, channel.getKey(), channel.getValue());
			System.out.printf("Subscribed to %s channel%n",
				channel.getKey());
		}
		// [<step-4]
		// Register the account and confirm it is active [>step-5]
		send(remote, "/w/api/account/get", JSON_MAPPER.createObjectNode()
			.put("account", monitorAddress).toString());
		while (true) {
			final StompMessage message = nextMessage();
			if (accountChannel.equals(message.destination())) {
				final String balance = JSON_MAPPER.readTree(message.body())
					.get("account").get("balance").asText();
				System.out.printf("Account update: balance=%s%n", balance);
				break;
			}
		}
		System.out.println("Account registered");
		// [<step-5]
		// Announce the transaction [>step-6]
		final String result = announceTransaction(payload,
			"/transaction/announce", "Announcing transaction "
				+ transactionHash.substring(0, 16) + "...");
		// [<step-6]
		// Wait for the transaction to confirm [>step-7]
		if ("SUCCESS".equals(result)) {
			boolean confirmed = false;
			while (true) {
				final StompMessage message = nextMessage();
				final JsonNode body = JSON_MAPPER.readTree(message.body());
				if (accountChannel.equals(message.destination())) {
					System.out.printf("Account update: balance=%s%n",
						body.get("account").get("balance").asText());
					if (confirmed)
						break;
				} else {
					final String hash = body.get("meta").get("hash")
						.get("data").asText();
					if (message.destination().contains("/transactions/")) {
						System.out.printf("confirmed: hash=%s...%n",
							hash.substring(0, 16));
						if (hash.equalsIgnoreCase(transactionHash)) {
							System.out.printf(
								"Transaction %s... confirmed%n",
								transactionHash.substring(0, 16));
							confirmed = true;
						}
					} else if (hash.equalsIgnoreCase(transactionHash)) {
						System.out.printf("unconfirmed: hash=%s...%n",
							hash.substring(0, 16));
					}
				}
			}
		} else {
			System.out.printf("Transaction rejected: %s%n", result);
		}
		// [<step-7]
		// Unsubscribe before closing [>step-8]
		for (final String id : channels.values())
			sendFrame(remote, "UNSUBSCRIBE\nid:" + id + "\n\n\0");
		System.out.println("Unsubscribed from all channels");
		sendFrame(remote, "DISCONNECT\n\n\0");
		session.close(); // [<step-8]
	}

	@OnMessage
	public void onMessage(final String frame) {
		rawFrames.add(frame);
	}

	private StompMessage nextMessage() throws Exception {
		while (true) {
			final String raw = rawFrames.take();
			if (!raw.startsWith("a"))
				continue;
			for (final JsonNode item
				: JSON_MAPPER.readTree(raw.substring(1))) {
				final String frame = item.asText();
				if (!frame.startsWith("MESSAGE\n"))
					continue;
				final int separator = frame.indexOf("\n\n");
				final String headers = frame.substring(0, separator);
				String destination = "";
				for (final String line : headers.split("\n"))
					if (line.startsWith("destination:"))
						destination = line.substring(12);
				final String body = frame.substring(separator + 2)
					.replace("\0", "");
				return new StompMessage(destination, body);
			}
		}
	}

	private static String sockJsUrl(final String endpoint) {
		final int server = ThreadLocalRandom.current().nextInt(100, 1000);
		return endpoint.replaceFirst("http", "ws") + "/" + server
			+ "/" + UUID.randomUUID().toString().replace("-", "")
			+ "/websocket";
	}

	private static void subscribe(
		final RemoteEndpoint.Basic remote,
		final String destination,
		final String id
	) throws IOException {
		sendFrame(remote, "SUBSCRIBE\nid:" + id + "\ndestination:"
			+ destination + "\nack:auto\n\n\0");
	}

	private static void send(
		final RemoteEndpoint.Basic remote,
		final String destination,
		final String body
	) throws IOException {
		sendFrame(remote, "SEND\ndestination:" + destination
			+ "\ncontent-type:application/json\ncontent-length:"
			+ body.length() + "\n\n" + body + "\0");
	}

	private static void sendFrame(
		final RemoteEndpoint.Basic remote,
		final String frame
	) throws IOException {
		final ArrayNode payload = JSON_MAPPER.createArrayNode();
		payload.add(frame);
		remote.sendText(payload.toString());
	}

	private record StompMessage(String destination, String body) {}
}

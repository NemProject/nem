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
import org.symbol.sdk.nem.descriptors.CosignatureV1Descriptor;
import org.symbol.sdk.nem.descriptors.MultisigTransactionV1Descriptor;
import org.symbol.sdk.nem.descriptors.TransferTransactionV2Descriptor;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.Transaction;

@ClientEndpoint
public final class ListenMultisigTransactionFlow {
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
			new ListenMultisigTransactionFlow().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws Exception {
		System.out.printf("Using node %s%n", nodeUrl);

		// Set up the multisig and cosignatory accounts [>step-1]
		final String multisigPublicKeyString = System.getenv()
			.getOrDefault(
			"MULTISIG_PUBLIC_KEY",
			"D656155B48D4E71E4C59EC6FAEB5EB4F214DE8BC3C65D5BF6A3D9931B4E5"
				+ "ACF2");
		final CryptoTypes.PublicKey multisigPublicKey =
			new CryptoTypes.PublicKey(multisigPublicKeyString);
		final String multisigAddress = facade.network.publicKeyToAddress(
			multisigPublicKey).toString();
		System.out.printf("Multisig address: %s%n", multisigAddress);
		final KeyPair cosignatory0KeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(System.getenv().getOrDefault(
				"COSIGNATORY0_PRIVATE_KEY", "0".repeat(63) + "2")));
		System.out.printf("Cosignatory 0 public key: %s%n",
			cosignatory0KeyPair.getPublicKey());
		final KeyPair cosignatory1KeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(System.getenv().getOrDefault(
				"COSIGNATORY1_PRIVATE_KEY", "0".repeat(63) + "3")));
		System.out.printf("Cosignatory 1 public key: %s%n",
			cosignatory1KeyPair.getPublicKey());  // [<step-1]
		// [>step-2]
		// [Cosignatory 0] Build and sign the multisig transaction
		final Transaction transferTransaction =
			facade.createTransactionFromTypedDescriptor(
				new TransferTransactionV2Descriptor(
					new Address(multisigAddress),
					new Amount(1_000_000), null, List.of()),
				multisigPublicKey, 0, 2 * 60 * 60);
		transferTransaction.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(transferTransaction)));
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new MultisigTransactionV1Descriptor(
					NemTransactionFactory.toNonVerifiableTransaction(
						transferTransaction), List.of()),
				cosignatory0KeyPair.getPublicKey(), 0, 2 * 60 * 60);
		transaction.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(transaction)));
		final String payload = NemTransactionFactory.attachSignature(
			transaction,
			facade.signTransaction(cosignatory0KeyPair, transaction));
		final String transactionHash = facade.hashTransaction(transaction)
			.toString().toUpperCase();
		final String shortHash = transactionHash.substring(0, 16);
		System.out.printf(
			"[Cosignatory 0] Built multisig transaction %s...%n",
			shortHash); // [<step-2]

		// [Cosignatory 1] Connect to the WebSocket [>step-3]
		final Session session = ContainerProvider.getWebSocketContainer()
			.connectToServer(this,
				URI.create(sockJsUrl(wsUrl + "/w/messages")));
		final RemoteEndpoint.Basic remote = session.getBasicRemote();
		rawFrames.take(); // consume the SockJS open frame
		sendFrame(remote, "CONNECT\naccept-version:1.1,1.0\n"
			+ "heart-beat:0,0\nhost:" + nodeUrl + "\n\n\0");
		System.out.printf("[Cosignatory 1] Connected to %s%n", wsUrl);
		// [<step-3]
		// [Cosignatory 1] Subscribe to the multisig account [>step-4]
		// channels
		final String accountChannel = "/account/" + multisigAddress;
		final Map<String, String> channels = new HashMap<>();
		channels.put(accountChannel, "id-0");
		channels.put("/unconfirmed/" + multisigAddress, "id-1");
		channels.put("/transactions/" + multisigAddress, "id-2");
		for (final Map.Entry<String, String> channel
			: channels.entrySet()) {
			subscribe(remote, channel.getKey(), channel.getValue());
			System.out.printf("[Cosignatory 1] Subscribed to %s channel%n",
				channel.getKey());
		} // [<step-4]

		// [Cosignatory 1] Register the multisig account [>step-5]
		send(remote, "/w/api/account/get", JSON_MAPPER.createObjectNode()
			.put("account", multisigAddress).toString());
		while (true) {
			final StompMessage message = nextMessage();
			if (accountChannel.equals(message.destination())) {
				final String balance = JSON_MAPPER.readTree(message.body())
					.get("account").get("balance").asText();
				System.out.printf("Account update: balance=%s%n", balance);
				break;
			}
		}
		System.out.println(
			"[Cosignatory 1] Multisig account registered"); // [<step-5]

		// [Cosignatory 0] Announce the multisig transaction [>step-6]
		final String result = announceTransaction(payload,
			"/transaction/announce",
			"[Cosignatory 0] Announcing multisig transaction "
				+ shortHash + "...");
		if ("SUCCESS".equals(result)) {
			// The transaction is now waiting for the second signature
			// [<step-6]
			String innerTransactionHash = null;
			boolean cosigned = false;
			boolean confirmed = false;
			while (true) {
				final StompMessage message = nextMessage();
				final JsonNode body = JSON_MAPPER.readTree(message.body());
				if (accountChannel.equals(message.destination())) {
					System.out.printf("Account update: balance=%s%n",
						body.get("account").get("balance").asText());
					if (confirmed)
						break;
				} else if (message.destination()
					.contains("/unconfirmed/") && !cosigned) {
					// Select the pending multisig transaction [>step-7]
					final JsonNode otherTransaction = body
						.get("transaction").get("otherTrans");
					if (null == otherTransaction
						|| !multisigPublicKeyString.equalsIgnoreCase(
							otherTransaction.get("signer").asText()))
						continue;
					innerTransactionHash = body.get("meta")
						.get("innerHash").get("data").asText();
					System.out.printf("unconfirmed: innerHash=%s...%n",
						innerTransactionHash.substring(0, 16));
					// [<step-7]
					// Cosign the pending transaction [>step-8]
					final Transaction cosignature =
						facade.createTransactionFromTypedDescriptor(
							new CosignatureV1Descriptor(
								new CryptoTypes.Hash256(
									innerTransactionHash),
								new Address(multisigAddress)),
							cosignatory1KeyPair.getPublicKey(),
							0, 2 * 60 * 60);
					cosignature.setFee(new Amount(
						FeeCalculator.calculateTransactionFee(
							cosignature)));
					final String cosignaturePayload =
						NemTransactionFactory.attachSignature(
							cosignature, facade.signTransaction(
								cosignatory1KeyPair, cosignature));
					final String cosignatureResult = announceTransaction(
						cosignaturePayload, "/transaction/announce",
						"[Cosignatory 1] Announced cosignature");
					if (!"SUCCESS".equals(cosignatureResult)) {
						System.out.printf("Cosignature rejected: %s%n",
							cosignatureResult);
						break;
					}
					cosigned = true; // [<step-8]
				} else if (message.destination()
					.contains("/transactions/")) {
					// [Cosignatory 1] Wait for confirmation [>step-9]
					final String messageHash = body.get("meta")
						.get("innerHash").get("data").asText();
					System.out.printf("confirmed: innerHash=%s...%n",
						messageHash.substring(0, 16));
					if (messageHash.equals(innerTransactionHash)
						&& !confirmed) {
						System.out.println(
							"Multisig transaction confirmed");
						confirmed = true;
					} // [<step-9]
				}
			}
		} else {
			System.out.printf("Transaction rejected: %s%n", result);
		}
		// [Cosignatory 1] Unsubscribe before closing [>step-10]
		for (final String id : channels.values())
			sendFrame(remote, "UNSUBSCRIBE\nid:" + id + "\n\n\0");
		System.out.println("Unsubscribed from all channels");
		sendFrame(remote, "DISCONNECT\n\n\0");
		session.close(); // [<step-10]
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

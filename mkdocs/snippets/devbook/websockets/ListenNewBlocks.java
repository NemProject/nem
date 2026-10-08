//JAVA 21+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//DEPS org.glassfish.tyrus.bundles:tyrus-standalone-client:2.2.0

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint
public final class ListenNewBlocks {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private final CompletableFuture<Void> openFuture =
		new CompletableFuture<>();

	private final String nodeUrl = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	private final String wsUrl = nodeUrl.replace(":7890", ":7778");

	public static void main(final String[] args) {
		try {
			new ListenNewBlocks().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws Exception {
		System.out.printf("Using node %s%n", nodeUrl);

		// Open connection [>step-1]
		final WebSocketContainer container =
			ContainerProvider.getWebSocketContainer();
		final Session session = container.connectToServer(
			this, URI.create(sockJsUrl(wsUrl + "/w/messages")));
		final RemoteEndpoint.Basic remote = session.getBasicRemote();
		openFuture.join();
		sendFrame(remote, "CONNECT\naccept-version:1.1,1.0\n"
			+ "heart-beat:0,0\nhost:" + wsUrl + "\n\n\0");
		System.out.printf("Connected to %s%n", wsUrl);
		// [<step-1]
		// Subscribe to the new block channel [>step-2]
		final String destination = "/blocks";
		sendFrame(remote, "SUBSCRIBE\nid:id-0\ndestination:"
			+ destination + "\nack:auto\n\n\0");
		System.out.printf("Subscribed to %s channel%n", destination);
		// [<step-2]
		// Unsubscribe on exit [>step-4]
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				sendFrame(remote, "UNSUBSCRIBE\nid:id-0\n\n\0");
				sendFrame(remote, "DISCONNECT\n\n\0");
				session.close();
				System.out.println("Unsubscribed and disconnected");
			} catch (final IOException ex) {
				throw new IllegalStateException(ex);
			}
		}));
		// [<step-4]
		new CompletableFuture<Void>().join();
	}

	// Read and format each new block [>step-3]
	@OnMessage
	public void onMessage(final String rawFrame) throws IOException {
		if ("o".equals(rawFrame)) {
			openFuture.complete(null);
			return;
		}
		if (!rawFrame.startsWith("a"))
			return;
		for (final JsonNode payload
			: JSON_MAPPER.readTree(rawFrame.substring(1))) {
			final String frame = payload.asText();
			if (!frame.startsWith("MESSAGE\n"))
				continue;
			final int bodyStart = frame.indexOf("\n\n") + 2;
			final String body = frame.substring(bodyStart)
				.replace("\0", "");
			final JsonNode block = JSON_MAPPER.readTree(body);
			System.out.printf(
				"New block: height=%,d harvester=%s...%n",
				block.get("height").asLong(),
				block.get("signer").asText().substring(0, 16)
					.toUpperCase());
		}
	} // [<step-3]

	private static String sockJsUrl(final String endpoint) {
		final int server = ThreadLocalRandom.current().nextInt(100, 1000);
		return endpoint.replaceFirst("http", "ws") + "/" + server
			+ "/" + UUID.randomUUID().toString().replace("-", "")
			+ "/websocket";
	}

	private static void sendFrame(
		final RemoteEndpoint.Basic remote,
		final String frame
	) throws IOException {
		final ArrayNode payload = JSON_MAPPER.createArrayNode();
		payload.add(frame);
		remote.sendText(payload.toString());
	}
}

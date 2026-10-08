//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ChainHeights {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private static final int REWRITE_LIMIT = 360;

	private final String nodeUrl = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	public static void main(final String[] args) {
		try {
			new ChainHeights().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", nodeUrl);

		Long prevHeight = null;
		Long heightChangedAt = null;

		while (true) {
			final HttpRequest request = // [>step-1]
				HttpRequest.newBuilder(
					URI.create(nodeUrl + "/chain/height")).GET().build();
			final HttpResponse<String> response = HTTP_CLIENT.send(
				request, BodyHandlers.ofString());
			if (200 != response.statusCode())
				throw new IOException("HTTP error! status: "
					+ response.statusCode());
			final JsonNode chainHeight = JSON_MAPPER.readTree(
				response.body());
			final long height = chainHeight.get("height").asLong();
			// [<step-1]
			// [>step-2]
			final long irreversibleHeight = Math.max(
				0, height - REWRITE_LIMIT);
			// [<step-2]
			final long now = System.currentTimeMillis(); // [>step-3]
			if (null != prevHeight && height != prevHeight)
				heightChangedAt = now;
			final String heightAgo = null != heightChangedAt
				? "%ds ago".formatted((now - heightChangedAt) / 1000)
				: "-"; // [<step-3]
			// [>step-4]
			System.out.printf(
				"Height: %,10d  (changed %s)"
					+ "  |  Irreversible: %,10d%n",
				height, heightAgo, irreversibleHeight);
			prevHeight = height;
			Thread.sleep(1000);
			// [<step-4]
		}
	}
}

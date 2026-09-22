//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.NetworkTimestamp;

public final class HelloWorld {
	public static void main(final String[] args) {
		try {
			new HelloWorld().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		// [>step-1]
		final NemFacade facade = new NemFacade("testnet");
		System.out.println("Network name: " + facade.network.name);
		// NetworkTimestamp(0) is the genesis block timestamp
		// (network launch)
		final Instant launchDate = facade.network.toDatetime(
			new NetworkTimestamp(0));
		System.out.println(
			"Network launch date: " + launchDate); // [<step-1]
		// [>step-2]
		final String nodeUrl = "http://libertalia.nemtest.net:7890";
		System.out.println("Using node " + nodeUrl);
		// Fetch current chain height
		final String heightPath = "/chain/height";
		System.out.println("Fetching chain height from " + heightPath);
		final HttpRequest request = HttpRequest
			.newBuilder(URI.create(nodeUrl + heightPath))
			.timeout(Duration.ofSeconds(10))
			.GET()
			.build();
		final HttpResponse<String> response = HttpClient.newHttpClient()
			.send(request, BodyHandlers.ofString());
		if (200 != response.statusCode())
			throw new IOException("HTTP error! status: "
				+ response.statusCode());
		final JsonNode responseJson = new ObjectMapper()
			.readTree(response.body());
		final long height = responseJson.get("height").asLong();
		System.out.printf(
			"  Blockchain height: %,d blocks%n", height); // [<step-2]
	}
}

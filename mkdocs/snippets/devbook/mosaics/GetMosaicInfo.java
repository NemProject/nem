//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class GetMosaicInfo {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private static final String NODE_URL = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	public static void main(final String[] args) {
		try {
			new GetMosaicInfo().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", NODE_URL);

		final String mosaicId = System.getenv().getOrDefault(
			"MOSAIC_ID", "nem:xem");
		System.out.printf("Mosaic ID: %s%n", mosaicId);

		// Fetch mosaic information [>step-1]
		final String mosaicPath = String.format(
			"/mosaic/definition?mosaicId=%s", mosaicId);
		System.out.printf("Fetching mosaic information from %s%n",
			mosaicPath);
		final HttpRequest mosaicRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + mosaicPath)).GET().build();
		final HttpResponse<String> mosaicResponse = HTTP_CLIENT.send(
			mosaicRequest, BodyHandlers.ofString());
		if (2 != mosaicResponse.statusCode() / 100)
			throw new IOException("HTTP error! status: "
				+ mosaicResponse.statusCode());
		final JsonNode mosaicJson = JSON_MAPPER.readTree(
			mosaicResponse.body());
		final JsonNode id = mosaicJson.get("id");
		final String fullName = String.format("%s:%s",
			id.get("namespaceId").asText(), id.get("name").asText());
		System.out.println("Mosaic information:");
		System.out.printf("  Mosaic ID: %s%n", fullName);
		System.out.printf("  Description: %s%n",
			mosaicJson.get("description").asText());
		System.out.printf("  Creator: %s%n",
			mosaicJson.get("creator").asText());
		final Map<String, String> properties = new HashMap<>();
		for (final JsonNode property : mosaicJson.get("properties")) {
			properties.put(property.get("name").asText(),
				property.get("value").asText());
		}
		final int divisibility = Integer.parseInt(
			properties.get("divisibility"));
		System.out.printf("  Divisibility: %d%n", divisibility);
		System.out.printf("  Initial supply: %s%n",
			properties.get("initialSupply"));
		System.out.printf("  Supply mutable: %s%n",
			properties.get("supplyMutable"));
		System.out.printf("  Transferable: %s%n",
			properties.get("transferable"));
		final JsonNode levy = mosaicJson.get("levy");
		System.out.printf("  Levy: %s%n",
			levy.isEmpty() ? "none" : levy.toString());
		// [<step-1]
		// Fetch the current supply [>step-2]
		final String supplyPath = String.format(
			"/mosaic/supply?mosaicId=%s", mosaicId);
		System.out.printf("%nFetching current supply from %s%n",
			supplyPath);
		final HttpRequest supplyRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + supplyPath)).GET().build();
		final HttpResponse<String> supplyResponse = HTTP_CLIENT.send(
			supplyRequest, BodyHandlers.ofString());
		final BigInteger supply = new BigInteger(
			JSON_MAPPER.readTree(supplyResponse.body())
				.get("supply").asText());
		System.out.printf("  Current supply: %s%n", supply);
		// [<step-2]
		// Convert the supply to atomic units [>step-3]
		final BigInteger atomic = supply.multiply(
			BigInteger.TEN.pow(divisibility));
		System.out.printf("%nSupply in atomic units: %s%n", atomic);
		// [<step-3]
	}
}

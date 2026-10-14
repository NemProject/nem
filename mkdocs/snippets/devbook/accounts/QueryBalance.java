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

public final class QueryBalance {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private static final String NODE_URL = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	// [>step-2]
	/**
	 * Fetch all mosaic balances owned by an account.
	 */
	private static JsonNode getMosaicBalances(
		final String address
	) throws IOException, InterruptedException {
		final String path = String.format(
			"/account/mosaic/owned?address=%s", address);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(NODE_URL + path)).GET().build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		return JSON_MAPPER.readTree(response.body()).get("data");
	} // [<step-2]

	// [>step-3]
	/**
	 * Fetch mosaic definitions for every mosaic owned by an account.
	 */
	private static Map<String, JsonNode> getMosaicDefinitions(
		final String address
	) throws IOException, InterruptedException {
		final String path = String.format(
			"/account/mosaic/owned/definition?address=%s", address);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(NODE_URL + path)).GET().build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		final JsonNode info = JSON_MAPPER.readTree(response.body());
		// Build a map from "namespace:name" to mosaic definition
		final Map<String, JsonNode> definitionsMap = new HashMap<>();
		for (final JsonNode entry : info.get("data")) {
			final JsonNode id = entry.get("id");
			final String key = String.format("%s:%s",
				id.get("namespaceId").asText(),
				id.get("name").asText());
			definitionsMap.put(key, entry);
		}
		return definitionsMap;
	} // [<step-3]

	// [>step-4]
	/**
	 * Format an atomic amount with decimal places.
	 */
	private static String formatAmount(
		final BigInteger amount,
		final int divisibility
	) {
		if (0 == divisibility)
			return amount.toString();

		final BigInteger divisor = BigInteger.TEN.pow(divisibility);
		final BigInteger[] parts = amount.divideAndRemainder(divisor);
		return String.format(
			"%s.%0" + divisibility + "d", parts[0], parts[1]);
	} // [<step-4]

	public static void main(final String[] args) {
		try {
			new QueryBalance().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", NODE_URL);

		// The account address to query [>step-5]
		final String address = System.getenv().getOrDefault(
			"ADDRESS", "TBONKWCOWBZYZB2I5JD3LSDBQVBYHB757VN3SKPP");
		System.out.printf("Fetching balances for %s%n", address);

		// Fetch mosaic balances and definitions for the account
		final JsonNode accountMosaics = getMosaicBalances(address);
		final Map<String, JsonNode> mosaicDefinitions =
			getMosaicDefinitions(address);

		if (accountMosaics.isEmpty()) {
			System.out.println("Account holds no mosaics");
		} else {
			System.out.printf("Account holds %d mosaic(s):%n",
				accountMosaics.size());
			for (final JsonNode mosaicEntry : accountMosaics) {
				final JsonNode mosaicId = mosaicEntry.get("mosaicId");
				final String key = String.format("%s:%s",
					mosaicId.get("namespaceId").asText(),
					mosaicId.get("name").asText());
				final BigInteger balance = new BigInteger(
					mosaicEntry.get("quantity").asText());

				// Get mosaic divisibility from the definition
				final JsonNode definition = mosaicDefinitions.get(key);
				int divisibility = 0;
				for (final JsonNode property
					: definition.get("properties")) {
					if ("divisibility".equals(
						property.get("name").asText()))
						divisibility = property.get("value").asInt();
				}

				// Format and display the balance
				final String formattedBalance = formatAmount(
					balance, divisibility);
				System.out.printf("- Mosaic %s%n", key);
				System.out.printf("  Balance: %s%n", formattedBalance);
				System.out.printf("  Balance (atomic): %s%n", balance);
				System.out.printf("  Divisibility: %d%n", divisibility);
			}
		} // [<step-5]
	}
}

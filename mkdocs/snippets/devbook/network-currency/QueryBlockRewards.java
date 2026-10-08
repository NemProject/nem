//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.Address;

public final class QueryBlockRewards {
	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final String NODE_URL = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	private static final String BLOCK_HEIGHT =
		System.getenv().getOrDefault("BLOCK_HEIGHT", "661258");

	// Format an atomic amount as whole XEM with integer math,
	// since amounts can exceed float precision.
	private static String fmt(final BigInteger value) {
		final BigInteger divisor = BigInteger.valueOf(1_000_000);
		final BigInteger[] parts = value.divideAndRemainder(divisor);
		return String.format(Locale.US, "%,d.%06d", parts[0], parts[1]);
	}

	public static void main(final String[] args) {
		try {
			new QueryBlockRewards().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", NODE_URL);
		final NemFacade facade = new NemFacade("testnet");

		// Fetch the block at the given height [>step-1]
		final ObjectNode requestBody = JSON_MAPPER.createObjectNode();
		requestBody.put("height", Long.parseLong(BLOCK_HEIGHT));
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(NODE_URL + "/block/at/public"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(
				requestBody.toString()))
			.build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		if (200 != response.statusCode())
			throw new IOException("HTTP error! status: "
				+ response.statusCode());
		final JsonNode block = JSON_MAPPER.readTree(response.body());
		final JsonNode transactions = block.get("transactions");
		System.out.printf("Block height: %s%n", BLOCK_HEIGHT);
		System.out.printf("Transactions: %d%n", transactions.size());
		// [<step-1]
		// Identify the harvester [>step-2]
		final Address harvester = facade.network.publicKeyToAddress(
			new CryptoTypes.PublicKey(block.get("signer").asText()));
		System.out.printf("Harvester: %s%n", harvester);
		// [<step-2]
		// Sum the transaction fees [>step-3]
		BigInteger totalReward = BigInteger.ZERO;
		System.out.println("\nTransaction fees:");
		for (final JsonNode transaction : transactions) {
			final BigInteger fee = new BigInteger(
				transaction.get("fee").asText());
			totalReward = totalReward.add(fee);
			System.out.printf("  Fee: %s XEM%n", fmt(fee));
		}
		// [<step-3]
		// Total reward [>step-4]
		System.out.printf("\nTotal block reward: %s XEM%n",
			fmt(totalReward));
		// [<step-4]
	}
}

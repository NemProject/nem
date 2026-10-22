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

public final class MonitoringStatus {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private static final String NODE_URL = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	// Query /transaction/get once to check for confirmation. [>step-2]
	private static Long getConfirmationHeight(
		final String transactionHash
	) throws IOException, InterruptedException {
		final String path = String.format(
			"/transaction/get?hash=%s", transactionHash);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(NODE_URL + path)).GET().build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		if (2 == response.statusCode() / 100)
			return JSON_MAPPER.readTree(response.body())
				.get("meta").get("height").asLong();
		if (400 != response.statusCode())
			throw new IOException(
				"Unexpected status: " + response.statusCode());
		return null;
	} // [<step-2]
	// [>step-3]
	// Check whether a transaction is in the address's unconfirmed pool.
	private static boolean isInUnconfirmedPool(
		final String signature,
		final String address
	) throws IOException, InterruptedException {
		final String path = String.format(
			"/account/unconfirmedTransactions?address=%s", address);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(NODE_URL + path)).GET().build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		final JsonNode pool = JSON_MAPPER.readTree(response.body())
			.get("data");
		for (final JsonNode entry : pool) {
			final String candidate = entry.get("transaction")
				.get("signature").asText();
			if (signature.equalsIgnoreCase(candidate))
				return true;
		}
		return false;
	} // [<step-3]

	// Check repeatedly until the transaction is confirmed. [>step-4]
	private static boolean waitForConfirmation(
		final String transactionHash,
		final int maxAttempts,
		final int waitSeconds
	) throws IOException, InterruptedException {
		System.out.println("\nWaiting for transaction confirmation");
		for (int attempt = 1; maxAttempts >= attempt; ++attempt) {
			Thread.sleep(waitSeconds * 1000L);
			final Long height = getConfirmationHeight(transactionHash);
			final String status = null == height
				? "pending"
				: "confirmed in block " + height;
			System.out.printf("  Attempt %d: %s%n", attempt, status);
			if (null != height)
				return true;
		}
		return false;
	} // [<step-4]

	public static void main(final String[] args) {
		try {
			new MonitoringStatus().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", NODE_URL);
		// [>step-1]
		// Transaction hash to monitor.
		final String transactionHash = System.getenv().getOrDefault(
			"TRANSACTION_HASH",
			"AE0B2142DFB75C9C126442EF612944E926BCE63FA34B353CDE409E2E87703"
				+ "C0B");
		// Signer's address.
		final String signerAddress = System.getenv().getOrDefault(
			"SIGNER_ADDRESS",
			"TBONKWCOWBZYZB2I5JD3LSDBQVBYHB757VN3SKPP");
		// Transaction signature.
		final String transactionSignature = System.getenv().getOrDefault(
			"TRANSACTION_SIGNATURE",
			"99B1850FADDB964112D030AA0A5C9F8B5B1B6B992B407D9C70"
				+ "F52F089BD651DFA7D4991639A48B810EFD98C45060D7AD9AE5"
				+ "7FDA37F58561459DCE8D0A747F02"); // [<step-1]
		System.out.printf("Monitoring transaction: %s%n", transactionHash);

		// [>step-5]
		final Long blockHeight = getConfirmationHeight(transactionHash);
		if (null != blockHeight)
			System.out.printf("%nTransaction confirmed in block %d%n",
				blockHeight);
		else if (!isInUnconfirmedPool(
			transactionSignature, signerAddress))
			System.out.println("\nTransaction not found");
		else if (waitForConfirmation(transactionHash, 120, 1))
			System.out.println("\nTransaction confirmed!");
		else
			System.out.println("\nConfirmation timed out"); // [<step-5]
	}
}

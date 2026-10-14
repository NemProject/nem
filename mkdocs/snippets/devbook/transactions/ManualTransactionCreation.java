//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.FeeCalculator;
import org.symbol.sdk.nem.KeyPair;
import org.symbol.sdk.nem.NemTransactionFactory;
import org.symbol.sdk.nem.NetworkTimestamp;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.Transaction;

public final class ManualTransactionCreation {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private final String nodeUrl = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	private final NemFacade facade = new NemFacade("testnet");

	// Helper method to announce a transaction
	private String announceTransaction(
		final String payload,
		final String label
	) throws IOException, InterruptedException {
		final String announcePath = "/transaction/announce";
		System.out.printf("Announcing %s to %s%n", label, announcePath);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(nodeUrl + announcePath))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(payload))
			.build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		if (2 != response.statusCode() / 100)
			throw new IOException("HTTP " + response.statusCode());
		final String result = JSON_MAPPER.readTree(response.body())
			.get("message").asText();
		System.out.printf("  Result: %s%n", result);
		return result;
	}

	// Helper method to wait for transaction confirmation
	private void waitForConfirmation(
		final String transactionHash,
		final String label
	) throws IOException, InterruptedException {
		final String statusPath = String.format(
			"/transaction/get?hash=%s", transactionHash);
		System.out.printf("Waiting for %s confirmation from %s%n",
			label, statusPath);
		boolean isConfirmed = false;
		for (int attempt = 1; 120 >= attempt; ++attempt) {
			final HttpRequest request = HttpRequest.newBuilder(
				URI.create(nodeUrl + statusPath)).GET().build();
			final HttpResponse<String> response = HTTP_CLIENT.send(
				request, BodyHandlers.ofString());
			if (2 != response.statusCode() / 100) {
				System.out.println("  Transaction status: pending");
				Thread.sleep(1000);
			} else {
				final JsonNode confirmed = JSON_MAPPER.readTree(
					response.body());
				System.out.printf("%s confirmed in block %s%n", label,
					confirmed.get("meta").get("height").asText());
				isConfirmed = true;
				break;
			}
		}
		if (!isConfirmed)
			System.out.printf("%s confirmation took too long.%n", label);
	}

	public static void main(final String[] args) {
		try {
			new ManualTransactionCreation().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", nodeUrl);
		// [>step-1]
		final String signerPrivateKey = System.getenv().getOrDefault(
			"SIGNER_PRIVATE_KEY", "0".repeat(64));
		final KeyPair signerKeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(signerPrivateKey));
		final String recipientAddress = System.getenv().getOrDefault(
			"RECIPIENT_ADDRESS",
			"TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4"); // [<step-1]

		// Define the amount of XEM to transfer [>step-2]
		final double xem = Double.parseDouble(
			System.getenv().getOrDefault("XEM_AMOUNT", "1"));
		final long amount = Math.round(xem * 1_000_000); // [<step-2]

		// Fetch current network time [>step-3]
		final String timePath = "/time-sync/network-time";
		System.out.printf("Fetching current network time from %s%n",
			timePath);
		final HttpRequest timeRequest = HttpRequest.newBuilder(
			URI.create(nodeUrl + timePath)).GET().build();
		final HttpResponse<String> timeResponse = HTTP_CLIENT.send(
			timeRequest, BodyHandlers.ofString());
		final JsonNode timeJson = JSON_MAPPER.readTree(
			timeResponse.body());
		final long networkTime =
			timeJson.get("receiveTimeStamp").asLong() / 1000;
		System.out.printf(
			"  Network time: %d s since the nemesis block%n", networkTime);
		final NetworkTimestamp timestamp = new NetworkTimestamp(
			networkTime);
		final long deadline = timestamp.addHours(2).timestamp; // [<step-3]

		// Build the transaction [>step-4]
		final Transaction transaction = facade.transactionFactory.create(
			Map.of(
				"type", "transfer_transaction_v2",
				"signerPublicKey",
					signerKeyPair.getPublicKey().toString(),
				"timestamp", timestamp.timestamp,
				"deadline", deadline,
				"recipientAddress", recipientAddress,
				"amount", amount)); // [<step-4]

		// Calculate and attach the transaction fee [>step-5]
		final long fee = FeeCalculator.calculateTransactionFee(
			transaction);
		transaction.setFee(new Amount(fee));
		System.out.printf("  Transaction fee: %s XEM%n",
			(double) fee / 1_000_000); // [<step-5]

		// Sign transaction and generate final payload [>step-6]
		final CryptoTypes.Signature signature = facade.signTransaction(
			signerKeyPair, transaction);
		final String jsonPayload = NemTransactionFactory.attachSignature(
			transaction, signature);
		System.out.println("Built transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(transaction.toJson())); // [<step-6]

		// Announce the transaction [>step-7]
		final String announceResult = announceTransaction(
			jsonPayload, "transaction"); // [<step-7]

		// Wait for confirmation [>step-8]
		if ("SUCCESS".equals(announceResult)) {
			final String transactionHash = facade.hashTransaction(
				transaction).toString();
			System.out.printf("Transaction hash: %s%n", transactionHash);
			waitForConfirmation(transactionHash, "transaction");
		} else {
			System.out.printf(
				"Transaction rejected: %s%n", announceResult);
		} // [<step-8]
	}
}

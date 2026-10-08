//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.Address;
import org.symbol.sdk.nem.FeeCalculator;
import org.symbol.sdk.nem.KeyPair;
import org.symbol.sdk.nem.NemTransactionFactory;
import org.symbol.sdk.nem.descriptors.*;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.Transaction;

public final class RegisterRootNamespace {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private final String nodeUrl = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	private final NemFacade facade = new NemFacade("testnet");

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
			new RegisterRootNamespace().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", nodeUrl);

		// [>step-1]
		final String privateKey = System.getenv().getOrDefault(
			"SIGNER_PRIVATE_KEY", "0".repeat(64));
		final KeyPair signerKeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(privateKey));
		System.out.printf("Signer address: %s%n",
			facade.network.publicKeyToAddress(
				signerKeyPair.getPublicKey()));
		// [<step-1]
		// Build the namespace name [>step-2]
		final String namespaceName = System.getenv().getOrDefault(
			"ROOT_NAMESPACE",
			"ns_" + System.currentTimeMillis() / 1000);
		System.out.printf("Creating root namespace: %s%n", namespaceName);
		// [<step-2]
		// Build the transaction [>step-3]
		final long rentalFee = FeeCalculator.calculateNamespaceRentalFee(
			true);
		System.out.printf("  Namespace lease fee: %s XEM%n",
			(double) rentalFee / 1_000_000);
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new NamespaceRegistrationTransactionV1Descriptor(
					new Address(
						"TAMESPACEWH4MKFMBCVFERDPOOP4FK7MTDJEYP35"),
					new Amount(rentalFee),
					namespaceName.getBytes(UTF_8),
					null),
				signerKeyPair.getPublicKey(), 0, 2 * 60 * 60);
		// [<step-3]
		// Calculate and attach the transaction fee [>step-4]
		final long fee = FeeCalculator.calculateTransactionFee(
			transaction);
		transaction.setFee(new Amount(fee));
		System.out.printf("  Transaction fee: %s XEM%n",
			(double) fee / 1_000_000);
		// [<step-4]
		// Sign transaction and generate final payload [>step-5]
		final CryptoTypes.Signature signature = facade.signTransaction(
			signerKeyPair, transaction);
		final String payload = NemTransactionFactory.attachSignature(
			transaction, signature);
		System.out.println("Built transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(transaction.toJson()));
		final String transactionHash = facade.hashTransaction(transaction)
			.toString();
		System.out.printf("Transaction hash: %s%n", transactionHash);

		// Announce the transaction
		final String result = announceTransaction(
			payload, "namespace registration");
		// [<step-5]
		// Wait for confirmation [>step-6]
		if ("SUCCESS".equals(result))
			waitForConfirmation(
				transactionHash, "namespace registration");
		else
			System.out.printf("Transaction rejected: %s%n", result);
		// [<step-6]
		// Retrieve the namespace [>step-7]
		final String path = "/namespace?namespace=" + namespaceName;
		System.out.printf(
			"Fetching namespace information from %s%n", path);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(nodeUrl + path)).GET().build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		final JsonNode info = JSON_MAPPER.readTree(response.body());
		System.out.println("Namespace information:");
		System.out.printf("  Name: %s%n", info.get("fqn").asText());
		System.out.printf("  Owner: %s%n", info.get("owner").asText());
		System.out.printf("  Registration height: %s%n",
			info.get("height").asText());
		// [<step-7]
	}
}

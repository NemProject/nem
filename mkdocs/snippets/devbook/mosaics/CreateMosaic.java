//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;

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

public final class CreateMosaic {
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
			new CreateMosaic().run();
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
		// Build the mosaic ID [>step-2]
		final String namespaceName = System.getenv().getOrDefault(
			"NAMESPACE", "my_namespace");
		final String mosaicName = System.getenv().getOrDefault(
			"MOSAIC", "token_" + System.currentTimeMillis() / 1000);
		final String mosaicId = namespaceName + ":" + mosaicName;
		System.out.printf("Creating mosaic: %s%n", mosaicId);
		// [<step-2]
		// Define the mosaic [>step-3]
		final MosaicDefinitionDescriptor mosaicDefinition =
			new MosaicDefinitionDescriptor(
				signerKeyPair.getPublicKey(),
				new MosaicIdDescriptor(
					new NamespaceIdDescriptor(
						namespaceName.getBytes(UTF_8)),
					mosaicName.getBytes(UTF_8)),
				"My tutorial mosaic".getBytes(UTF_8),
				List.of(
					Map.entry("divisibility", "2"),
					Map.entry("initialSupply", "1000"),
					Map.entry("supplyMutable", "true"),
					Map.entry("transferable", "true"))
					.stream()
					.map(property ->
						new SizePrefixedMosaicPropertyDescriptor(
							new MosaicPropertyDescriptor(
								property.getKey().getBytes(UTF_8),
								property.getValue().getBytes(UTF_8))))
					.toList(),
				null);
		// [<step-3]
		// Build the mosaic definition transaction [>step-4]
		final long rentalFee = FeeCalculator.calculateMosaicRentalFee();
		System.out.printf("  Mosaic creation fee: %s XEM%n",
			(double) rentalFee / 1_000_000);
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new MosaicDefinitionTransactionV1Descriptor(
					mosaicDefinition,
					new Address(
						"TBMOSAICOD4F54EE5CDMR23CCBGOAM2XSJBR5OLC"),
					new Amount(rentalFee)),
				signerKeyPair.getPublicKey(), 0, 2 * 60 * 60);
		// [<step-4]
		// Calculate and attach the transaction fee [>step-5]
		final long fee = FeeCalculator.calculateTransactionFee(
			transaction);
		transaction.setFee(new Amount(fee));
		System.out.printf("  Transaction fee: %s XEM%n",
			(double) fee / 1_000_000);
		// [<step-5]
		// Sign and generate final payload [>step-6]
		final CryptoTypes.Signature signature = facade.signTransaction(
			signerKeyPair, transaction);
		final String payload = NemTransactionFactory.attachSignature(
			transaction, signature);
		System.out.println("Built mosaic definition transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(transaction.toJson()));

		// Announce the transaction
		final String result = announceTransaction(
			payload, "mosaic definition");
		// [<step-6]
		// Wait for confirmation [>step-7]
		if ("SUCCESS".equals(result)) {
			final String transactionHash = facade.hashTransaction(
				transaction).toString();
			System.out.printf("Transaction hash: %s%n", transactionHash);
			waitForConfirmation(
				transactionHash, "mosaic definition");
		} else {
			System.out.printf("Transaction rejected: %s%n", result);
		}
		// [<step-7]
		// Retrieve the mosaic [>step-8]
		final String path = "/mosaic/definition?mosaicId=" + mosaicId;
		System.out.printf("Fetching mosaic information from %s%n", path);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(nodeUrl + path)).GET().build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		final JsonNode info = JSON_MAPPER.readTree(response.body());
		System.out.println("Mosaic information:");
		System.out.printf("  Creator: %s%n",
			info.get("creator").asText());
		for (final JsonNode item : info.get("properties"))
			System.out.printf("  %s: %s%n",
				item.get("name").asText(), item.get("value").asText());
		// [<step-8]
	}
}

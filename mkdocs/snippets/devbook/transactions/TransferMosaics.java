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
import org.symbol.sdk.nem.FeeCalculator.MosaicInformation;
import org.symbol.sdk.nem.KeyPair;
import org.symbol.sdk.nem.NemTransactionFactory;
import org.symbol.sdk.nem.descriptors.MosaicDescriptor;
import org.symbol.sdk.nem.descriptors.MosaicIdDescriptor;
import org.symbol.sdk.nem.descriptors.NamespaceIdDescriptor;
import org.symbol.sdk.nem.descriptors.SizePrefixedMosaicDescriptor;
import org.symbol.sdk.nem.descriptors.TransferTransactionV2Descriptor;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.Transaction;

public final class TransferMosaics {
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
			new TransferMosaics().run();
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
			"TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4");
		// [<step-1]
		// [>step-2]
		final String mosaicId = System.getenv().getOrDefault(
			"MOSAIC_ID", "company:token");
		final String[] mosaicIdParts = mosaicId.split(":");
		final String mosaicNamespace = mosaicIdParts[0];
		final String mosaicName = mosaicIdParts[1];
		final int quantity = Integer.parseInt(
			System.getenv().getOrDefault("QUANTITY", "100"));
		System.out.printf("Sending mosaic %s%n", mosaicId);
		System.out.printf("  Amount: %d units%n", quantity);
		// [<step-2]

		// Fetch the mosaic's divisibility and supply [>step-3]
		final String definitionPath = String.format(
			"/mosaic/definition?mosaicId=%s", mosaicId);
		System.out.printf("Fetching mosaic definition from %s%n",
			definitionPath);
		final HttpRequest definitionRequest = HttpRequest.newBuilder(
			URI.create(nodeUrl + definitionPath)).GET().build();
		final HttpResponse<String> definitionResponse = HTTP_CLIENT.send(
			definitionRequest, BodyHandlers.ofString());
		final JsonNode definition = JSON_MAPPER.readTree(
			definitionResponse.body());
		int divisibility = 0;
		for (final JsonNode property : definition.get("properties")) {
			if ("divisibility".equals(property.get("name").asText()))
				divisibility = property.get("value").asInt();
		}
		final String supplyPath = String.format(
			"/mosaic/supply?mosaicId=%s", mosaicId);
		System.out.printf("Fetching mosaic supply from %s%n", supplyPath);
		final HttpRequest supplyRequest = HttpRequest.newBuilder(
			URI.create(nodeUrl + supplyPath)).GET().build();
		final HttpResponse<String> supplyResponse = HTTP_CLIENT.send(
			supplyRequest, BodyHandlers.ofString());
		final long supply = JSON_MAPPER.readTree(supplyResponse.body())
			.get("supply").asLong();
		System.out.printf("  %s: divisibility %d, supply %d%n",
			mosaicId, divisibility, supply);
		// [<step-3]
		// Build the transaction [>step-4]
		final long atomicQuantity = Math.round(
			quantity * Math.pow(10, divisibility));
		final long scaledMultiplier = 1_000_000;
		final MosaicIdDescriptor mosaicIdDescriptor =
			new MosaicIdDescriptor(
				new NamespaceIdDescriptor(mosaicNamespace.getBytes(UTF_8)),
				mosaicName.getBytes(UTF_8));
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new TransferTransactionV2Descriptor(
					new Address(recipientAddress),
					new Amount(scaledMultiplier),
					null,
					List.of(new SizePrefixedMosaicDescriptor(
						new MosaicDescriptor(
							mosaicIdDescriptor,
							new Amount(atomicQuantity))))),
				signerKeyPair.getPublicKey(),
				0,
				2 * 60 * 60);
		// [<step-4]
		// Calculate and attach the transaction fee [>step-5]
		final long fee = FeeCalculator.calculateTransactionFee(
			transaction, Map.of(mosaicId,
				new MosaicInformation(supply, divisibility)));
		transaction.setFee(new Amount(fee));
		System.out.printf("  Transaction fee: %s XEM%n",
			(double) fee / 1_000_000);
		// [<step-5]
		// Sign transaction and generate final payload [>step-6]
		final CryptoTypes.Signature signature = facade.signTransaction(
			signerKeyPair, transaction);
		final String jsonPayload = NemTransactionFactory.attachSignature(
			transaction, signature);
		System.out.println("Built transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(transaction.toJson()));
		// [<step-6]
		// Announce the transaction [>step-7]
		final String announceResult = announceTransaction(
			jsonPayload, "transaction");
		// [<step-7]
		// Wait for confirmation [>step-8]
		if ("SUCCESS".equals(announceResult)) {
			final String transactionHash = facade.hashTransaction(
				transaction).toString();
			System.out.printf("Transaction hash: %s%n", transactionHash);
			waitForConfirmation(transactionHash, "transaction");
		} else {
			System.out.printf(
				"Transaction rejected: %s%n", announceResult);
		}
		// [<step-8]
	}
}

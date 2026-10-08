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
import org.symbol.sdk.nem.FeeCalculator;
import org.symbol.sdk.nem.KeyPair;
import org.symbol.sdk.nem.NemTransactionFactory;
import org.symbol.sdk.nem.descriptors.*;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.MosaicSupplyChangeAction;
import org.symbol.sdk.nem.models.Transaction;

public final class ChangeMosaicSupply {
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

	private long fetchSupply(
		final String mosaicId
	) throws IOException, InterruptedException {
		final String path = String.format(
			"/mosaic/supply?mosaicId=%s", mosaicId);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(nodeUrl + path)).GET().build();
		final HttpResponse<String> response = HTTP_CLIENT.send(
			request, BodyHandlers.ofString());
		return JSON_MAPPER.readTree(response.body())
			.get("supply").asLong();
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
			new ChangeMosaicSupply().run();
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
		final String namespaceName = System.getenv().getOrDefault(
			"NAMESPACE", "my_namespace");
		final String mosaicName = System.getenv().getOrDefault(
			"MOSAIC", "token");
		final String mosaicId = namespaceName + ":" + mosaicName;
		System.out.printf("Mosaic ID: %s%n", mosaicId);
		// [<step-1]
		final MosaicIdDescriptor mosaicIdDescriptor =
			new MosaicIdDescriptor(
				new NamespaceIdDescriptor(namespaceName.getBytes(UTF_8)),
				mosaicName.getBytes(UTF_8));

		// --- INCREASING SUPPLY (MINTING) ---
		System.out.println("\n--- Increasing supply (minting) ---");
		// [>step-2]
		System.out.printf("Supply before minting: %d%n",
			fetchSupply(mosaicId));
		final Transaction increaseTx =
			facade.createTransactionFromTypedDescriptor(
				new MosaicSupplyChangeTransactionV1Descriptor(
					mosaicIdDescriptor,
					MosaicSupplyChangeAction.INCREASE,
					new Amount(500)),
				signerKeyPair.getPublicKey(), 0, 2 * 60 * 60);
		increaseTx.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(increaseTx)));

		final CryptoTypes.Signature increaseSignature =
			facade.signTransaction(signerKeyPair, increaseTx);
		final String increasePayload =
			NemTransactionFactory.attachSignature(
				increaseTx, increaseSignature);
		System.out.println("Built supply increase transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(increaseTx.toJson()));
		final String increaseResult = announceTransaction(
			increasePayload, "supply increase");
		if ("SUCCESS".equals(increaseResult)) {
			waitForConfirmation(facade.hashTransaction(increaseTx)
				.toString(), "supply increase");
			System.out.printf("Supply after minting: %d%n",
				fetchSupply(mosaicId));
		} else {
			System.out.println("Supply increase rejected");
		}
		// [<step-2]
		// --- DECREASING SUPPLY (BURNING) ---
		System.out.println("\n--- Decreasing supply (burning) ---");
		// [>step-3]
		final Transaction decreaseTx =
			facade.createTransactionFromTypedDescriptor(
				new MosaicSupplyChangeTransactionV1Descriptor(
					mosaicIdDescriptor,
					MosaicSupplyChangeAction.DECREASE,
					new Amount(500)),
				signerKeyPair.getPublicKey(), 0, 2 * 60 * 60);
		decreaseTx.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(decreaseTx)));

		final CryptoTypes.Signature decreaseSignature =
			facade.signTransaction(signerKeyPair, decreaseTx);
		final String decreasePayload =
			NemTransactionFactory.attachSignature(
				decreaseTx, decreaseSignature);
		System.out.println("Built supply decrease transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(decreaseTx.toJson()));
		final String decreaseResult = announceTransaction(
			decreasePayload, "supply decrease");
		if ("SUCCESS".equals(decreaseResult)) {
			waitForConfirmation(facade.hashTransaction(decreaseTx)
				.toString(), "supply decrease");
			System.out.printf("Supply after burning: %d%n",
				fetchSupply(mosaicId));
		} else {
			System.out.println("Supply decrease rejected");
		} // [<step-3]
	}
}

//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.Address;
import org.symbol.sdk.nem.FeeCalculator;
import org.symbol.sdk.nem.KeyPair;
import org.symbol.sdk.nem.NemTransactionFactory;
import org.symbol.sdk.nem.descriptors.CosignatureV1Descriptor;
import org.symbol.sdk.nem.descriptors.MultisigTransactionV1Descriptor;
import org.symbol.sdk.nem.descriptors.TransferTransactionV2Descriptor;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.Transaction;

public final class SignMultisig {
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
			new SignMultisig().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", nodeUrl);

		// [>step-1]
		final String multisigPublicKeyString = System.getenv()
			.getOrDefault(
			"MULTISIG_PUBLIC_KEY",
			"D656155B48D4E71E4C59EC6FAEB5EB4F214DE8BC3C65D5BF6A3D9931B4E5"
				+ "ACF2");
		final CryptoTypes.PublicKey multisigPublicKey =
			new CryptoTypes.PublicKey(multisigPublicKeyString);
		final Address multisigAddress = facade.network.publicKeyToAddress(
			multisigPublicKey);
		System.out.printf("Multisig public key: %s%n", multisigPublicKey);
		final KeyPair cosignatory0KeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(System.getenv().getOrDefault(
				"COSIGNATORY0_PRIVATE_KEY", "0".repeat(63) + "2")));
		System.out.printf("Cosignatory 0 public key: %s%n",
			cosignatory0KeyPair.getPublicKey());
		final KeyPair cosignatory1KeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(System.getenv().getOrDefault(
				"COSIGNATORY1_PRIVATE_KEY", "0".repeat(63) + "3")));
		System.out.printf("Cosignatory 1 public key: %s%n",
			cosignatory1KeyPair.getPublicKey()); // [<step-1]

		// Build the inner transfer transaction [>step-2]
		final Transaction transferTransaction =
			facade.createTransactionFromTypedDescriptor(
				new TransferTransactionV2Descriptor(
					new Address(multisigAddress.toString()),
					new Amount(1_000_000), null, List.of()), // 1 XEM
				multisigPublicKey, 0, 2 * 60 * 60);
		transferTransaction.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(transferTransaction)));
		// [<step-2]
		// Build the wrapper multisig transaction [>step-3]
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new MultisigTransactionV1Descriptor(
					NemTransactionFactory.toNonVerifiableTransaction(
						transferTransaction),
					List.of()),
				// This is the cosignatory that initiates the transfer
				cosignatory0KeyPair.getPublicKey(), 0, 2 * 60 * 60);
		transaction.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(transaction)));
		// [<step-3]
		// Sign and announce the multisig transaction [>step-4]
		final CryptoTypes.Signature signature = facade.signTransaction(
			cosignatory0KeyPair, transaction);
		final String payload = NemTransactionFactory.attachSignature(
			transaction, signature);
		System.out.println("Built multisig transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(transaction.toJson()));
		final String transactionHash = facade.hashTransaction(transaction)
			.toString();
		System.out.printf("Transaction hash: %s%n", transactionHash);
		final String announceResult = announceTransaction(
			payload, "multisig transaction");
		// The transaction is now waiting for the second signature
		// [<step-4]
		// Retrieve the pending transaction from the network [>step-5]
		if ("SUCCESS".equals(announceResult)) {
			final Address cosignatory1Address =
				facade.network.publicKeyToAddress(
					cosignatory1KeyPair.getPublicKey());
			final String path = String.format(
				"/account/unconfirmedTransactions?address=%s",
				cosignatory1Address);
			System.out.printf("Fetching pending transactions from %s%n",
				path);
			final HttpRequest request = HttpRequest.newBuilder(
				URI.create(nodeUrl + path)).GET().build();
			final JsonNode pending = JSON_MAPPER.readTree(HTTP_CLIENT.send(
				request, BodyHandlers.ofString()).body()).get("data");
			// Select the pending transaction issued by the multisig
			JsonNode pendingEntry = null;
			for (final JsonNode entry : pending) {
				final JsonNode otherTransaction = entry.get("transaction")
					.get("otherTrans");
				if (null != otherTransaction
					&& multisigPublicKeyString.equalsIgnoreCase(
						otherTransaction.get("signer").asText())) {
					pendingEntry = entry;
					break;
				}
			}
			if (null == pendingEntry)
				throw new IOException("Pending transaction not found");
			final String innerTransactionHash = pendingEntry.get("meta")
				.get("data").asText();
			System.out.printf("  Inner transaction hash: %s%n",
				innerTransactionHash);
			// [<step-5]
			// Build the cosignature [>step-6]
			final Transaction cosignature =
				facade.createTransactionFromTypedDescriptor(
					new CosignatureV1Descriptor(
						// Hash of the inner transfer transaction
						new CryptoTypes.Hash256(innerTransactionHash),
						// Address of the multisig account
						new Address(multisigAddress.toString())),
					// Cosignatory providing the second signature
					cosignatory1KeyPair.getPublicKey(), 0, 2 * 60 * 60);
			cosignature.setFee(new Amount(
				FeeCalculator.calculateTransactionFee(cosignature)));
			// [<step-6]
			// Sign and announce the cosignature [>step-7]
			final CryptoTypes.Signature cosignatureSignature =
				facade.signTransaction(cosignatory1KeyPair, cosignature);
			final String cosignaturePayload =
				NemTransactionFactory.attachSignature(
					cosignature, cosignatureSignature);
			System.out.println("Built cosignature:");
			System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
				.writeValueAsString(cosignature.toJson()));
			final String cosignatureResult = announceTransaction(
				cosignaturePayload, "cosignature");
			// [<step-7]
			// Wait for the multisig transaction to be confirmed [>step-8]
			if ("SUCCESS".equals(cosignatureResult))
				waitForConfirmation(
					transactionHash, "multisig transaction");
			else
				System.out.printf("Transaction rejected: %s%n",
					cosignatureResult);
			// [<step-8]
		} else {
			System.out.printf(
				"Transaction rejected: %s%n", announceResult);
		}
	}
}

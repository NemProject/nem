//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

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
import org.symbol.sdk.nem.models.MultisigAccountModificationType;
import org.symbol.sdk.nem.models.Transaction;

public final class ConfigureMultisig {
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
			new ConfigureMultisig().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", nodeUrl);

		// [>step-1]
		final String keyPrefix = "0".repeat(63);
		final String multisigPrivateKey = System.getenv().getOrDefault(
			"MULTISIG_PRIVATE_KEY", keyPrefix + "1");
		final KeyPair multisigKeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(multisigPrivateKey));
		final Address multisigAddress = facade.network.publicKeyToAddress(
			multisigKeyPair.getPublicKey());
		System.out.printf("Multisig address: %s (public key %s)%n",
			multisigAddress, multisigKeyPair.getPublicKey());
		final List<KeyPair> cosignatoryKeyPairs = new ArrayList<>();
		for (int i = 0; 2 > i; ++i) {
			final String privateKey = System.getenv().getOrDefault(
				String.format("COSIGNATORY%d_PRIVATE_KEY", i),
				keyPrefix + (i + 2));
			final KeyPair keyPair = new KeyPair(
				new CryptoTypes.PrivateKey(privateKey));
			cosignatoryKeyPairs.add(keyPair);
			final Address address = facade.network.publicKeyToAddress(
				keyPair.getPublicKey());
			System.out.printf(
				"Cosignatory %d address: %s (public key %s)%n",
				i, address, keyPair.getPublicKey());
		}
		// [<step-1]
		// Get the current state of the multisig account
		final List<String> cosignatories = getMultisigCosignatories(
			multisigAddress);
		// Decide which operation to perform [>step-3]
		final List<Transaction> transactions;
		if (cosignatories.isEmpty()) {
			transactions = List.of(multisigEnableTransaction(
				multisigKeyPair, cosignatoryKeyPairs, 1));
		} else {
			transactions = List.of(
				multisigRemovalTransaction(multisigKeyPair,
					cosignatoryKeyPairs, cosignatoryKeyPairs.get(1), 0),
				multisigRemovalTransaction(multisigKeyPair,
					cosignatoryKeyPairs, cosignatoryKeyPairs.get(0), -1));
		}
		// [<step-3]
		// Announce each transaction and wait for confirmation [>step-11]
		for (final Transaction transaction : transactions) {
			final String hash = facade.hashTransaction(transaction)
				.toString();
			System.out.printf("Built transaction with hash: %s%n", hash);
			final String result = announceTransaction(
				NemTransactionFactory.toJson(transaction), "transaction");
			if (!"SUCCESS".equals(result)) {
				System.out.println("Transaction rejected");
				break;
			}
			waitForConfirmation(hash, "transaction");
		}
		// [<step-11]
	}

	// Returns the cosignatory addresses of the provided multisig [>step-2]
	// account, or an empty list if the account is not multisig
	private List<String> getMultisigCosignatories(
		final Address address
	) throws IOException, InterruptedException {
		final String path = "/account/get?address=" + address;
		System.out.printf("Getting cosignatories from %s%n", path);
		final HttpRequest request = HttpRequest.newBuilder(
			URI.create(nodeUrl + path)).GET().build();
		final JsonNode found = JSON_MAPPER.readTree(HTTP_CLIENT.send(
			request, BodyHandlers.ofString()).body())
			.get("meta").get("cosignatories");
		final List<String> result = new ArrayList<>();
		for (final JsonNode cosignatory : found)
			result.add(cosignatory.get("address").asText());
		if (result.isEmpty())
			System.out.println("  Response: No cosignatories");
		else
			System.out.printf("  Response: %s%n", result);
		return result;
	}
	// [<step-2]

	// [>step-4]
	// Returns a transaction that turns a regular account into a multisig
	private Transaction multisigEnableTransaction(
		final KeyPair multisigKeyPair,
		final List<KeyPair> cosignatoryKeyPairs,
		final int approvalDelta
	) throws IOException {
		final List<SizePrefixedMultisigAccountModificationDescriptor>
			modifications = new ArrayList<>();
		for (final KeyPair keyPair : cosignatoryKeyPairs)
			modifications.add(
				new SizePrefixedMultisigAccountModificationDescriptor(
					new MultisigAccountModificationDescriptor(
						MultisigAccountModificationType.ADD_COSIGNATORY,
						keyPair.getPublicKey())));
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new MultisigAccountModificationTransactionV2Descriptor(
					approvalDelta, modifications),
				multisigKeyPair.getPublicKey(), 0, 2 * 60 * 60);
		// [<step-4]
		// Calculate and attach the transaction fee [>step-5]
		final long fee = FeeCalculator.calculateTransactionFee(
			transaction);
		transaction.setFee(new Amount(fee));
		System.out.printf("  Transaction fee: %s XEM%n",
			(double) fee / 1_000_000);
		System.out.println(
			"Enabling the multisig with the modification transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(transaction.toJson()));
		// [<step-5]
		// Sign the transaction with the multisig's key [>step-6]
		NemTransactionFactory.attachSignature(transaction,
			facade.signTransaction(multisigKeyPair, transaction));
		return transaction; // [<step-6]
	}

	// [>step-7]
	// Returns a transaction that removes one cosignatory from the multisig
	private Transaction multisigRemovalTransaction(
		final KeyPair multisigKeyPair,
		final List<KeyPair> cosignatoryKeyPairs,
		final KeyPair removedKeyPair,
		final int approvalDelta
	) throws IOException {
		final SizePrefixedMultisigAccountModificationDescriptor removal =
			new SizePrefixedMultisigAccountModificationDescriptor(
				new MultisigAccountModificationDescriptor(
					MultisigAccountModificationType.DELETE_COSIGNATORY,
					removedKeyPair.getPublicKey()));
		final Transaction innerTransaction =
			facade.createTransactionFromTypedDescriptor(
				new MultisigAccountModificationTransactionV2Descriptor(
					approvalDelta, List.of(removal)),
				multisigKeyPair.getPublicKey(), 0, 2 * 60 * 60);
		// [<step-7]
		// Wrap the modification in a multisig transaction [>step-8]
		final long innerFee = FeeCalculator.calculateTransactionFee(
			innerTransaction);
		innerTransaction.setFee(new Amount(innerFee));
		final Transaction transaction =
			facade.createTransactionFromTypedDescriptor(
				new MultisigTransactionV1Descriptor(
					NemTransactionFactory.toNonVerifiableTransaction(
						innerTransaction), List.of()),
				cosignatoryKeyPairs.get(0).getPublicKey(),
				0, 2 * 60 * 60);
		// [<step-8]
		// Calculate and attach the transaction fee [>step-9]
		final long fee = FeeCalculator.calculateTransactionFee(
			transaction);
		transaction.setFee(new Amount(fee));
		System.out.printf("  Transaction fee: %s XEM%n",
			(double) (innerFee + fee) / 1_000_000);
		System.out.println(
			"Disabling the multisig with the multisig transaction:");
		System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter()
			.writeValueAsString(transaction.toJson()));
		// [<step-9]
		// Sign the transaction with the cosignatory's key [>step-10]
		NemTransactionFactory.attachSignature(transaction,
			facade.signTransaction(
				cosignatoryKeyPairs.get(0), transaction));
		return transaction; // [<step-10]
	}
}

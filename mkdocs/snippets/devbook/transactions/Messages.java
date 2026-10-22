//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HexFormat;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.symbol.sdk.CryptoTypes;
import org.symbol.sdk.MessageEncoderResult;
import org.symbol.sdk.facade.NemFacade;
import org.symbol.sdk.nem.Address;
import org.symbol.sdk.nem.FeeCalculator;
import org.symbol.sdk.nem.KeyPair;
import org.symbol.sdk.nem.MessageEncoder;
import org.symbol.sdk.nem.NemTransactionFactory;
import org.symbol.sdk.nem.descriptors.*;
import org.symbol.sdk.nem.models.Amount;
import org.symbol.sdk.nem.models.Message;
import org.symbol.sdk.nem.models.MessageType;
import org.symbol.sdk.nem.models.Transaction;

public final class Messages {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	// Configuration
	private final String nodeUrl = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	private final NemFacade facade = new NemFacade("testnet");

	private JsonNode waitForConfirmation(
		final String transactionHash,
		final String label
	) throws IOException, InterruptedException {
		final String statusPath = String.format(
			"/transaction/get?hash=%s", transactionHash);
		System.out.printf("Waiting for %s confirmation from %s%n",
			label, statusPath);
		boolean isConfirmed = false;
		JsonNode confirmed = null;
		for (int attempt = 1; 120 >= attempt; ++attempt) {
			final HttpRequest request = HttpRequest.newBuilder(
				URI.create(nodeUrl + statusPath)).GET().build();
			final HttpResponse<String> response = HTTP_CLIENT.send(
				request, BodyHandlers.ofString());
			if (2 != response.statusCode() / 100) {
				System.out.println("  Transaction status: pending");
				Thread.sleep(1000);
			} else {
				confirmed = JSON_MAPPER.readTree(response.body());
				System.out.printf("%s confirmed in block %s%n", label,
					confirmed.get("meta").get("height").asText());
				isConfirmed = true;
				break;
			}
		}
		if (!isConfirmed)
			System.out.printf("%s confirmation took too long.%n", label);
		return confirmed;
	}

	public static void main(final String[] args) {
		try {
			new Messages().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", nodeUrl);

		// Set up sender and recipient accounts [>step-1]
		final String senderPrivateKeyString = System.getenv().getOrDefault(
			"SENDER_PRIVATE_KEY", "0".repeat(64));
		final KeyPair senderKeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(senderPrivateKeyString));
		final Address senderAddress = facade.network.publicKeyToAddress(
			senderKeyPair.getPublicKey());

		final String recipientPrivateKeyString = System.getenv()
			.getOrDefault("RECIPIENT_PRIVATE_KEY", "1".repeat(64));
		final KeyPair recipientKeyPair = new KeyPair(
			new CryptoTypes.PrivateKey(recipientPrivateKeyString));
		final Address recipientAddress = facade.network.publicKeyToAddress(
			recipientKeyPair.getPublicKey());

		System.out.printf("Sender address: %s%n", senderAddress);
		System.out.printf("Recipient address: %s%n%n", recipientAddress);
		// [<step-1]
		// --- PLAIN TEXT MESSAGE ---
		System.out.println("==> Sending Plain Text Message"); // [>step-2]

		// Create a plain text message
		final byte[] plainMessage = "Hello, NEM!".getBytes(UTF_8);
		System.out.printf("Plain message: %s%n",
			new String(plainMessage, UTF_8));

		// Build transfer transaction with plain message
		final Transaction plainTransaction =
			facade.createTransactionFromTypedDescriptor(
				new TransferTransactionV2Descriptor(
					recipientAddress,
					new Amount(0),
					new MessageDescriptor(
						MessageType.PLAIN, plainMessage),
					List.of()),
				senderKeyPair.getPublicKey(),
				0,
				2 * 60 * 60); // [<step-2]
		plainTransaction.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(plainTransaction)));

		// Sign and announce the transaction
		final CryptoTypes.Signature plainSignature =
			facade.signTransaction(senderKeyPair, plainTransaction);
		final String plainJsonPayload = NemTransactionFactory
			.attachSignature(plainTransaction, plainSignature);
		final String plainTransactionHash = facade.hashTransaction(
			plainTransaction).toString();
		System.out.printf("Transaction hash: %s%n", plainTransactionHash);

		final HttpRequest plainRequest = HttpRequest.newBuilder(
			URI.create(nodeUrl + "/transaction/announce"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(plainJsonPayload))
			.build();
		HTTP_CLIENT.send(plainRequest, BodyHandlers.ofString());
		System.out.println("Plain message transaction announced\n");

		// --- RECEIVING PLAIN TEXT MESSAGE ---
		// [>step-3]
		System.out.println("<== Receiving Plain Text Message");

		// Wait for confirmation
		final JsonNode plainTxData = waitForConfirmation(
			plainTransactionHash, "Plain message transaction");

		// Decode plain message from confirmed transaction
		final byte[] receivedPlainMessage = HexFormat.of().parseHex(
			plainTxData.get("transaction").get("message")
				.get("payload").asText());
		System.out.printf("Received plain message: %s%n%n",
			new String(receivedPlainMessage, UTF_8));
		// [<step-3]
		// --- ENCRYPTED MESSAGE ---
		System.out.println("==> Sending Encrypted Message"); // [>step-4]

		// Create a message encoder with sender's key pair
		final MessageEncoder senderMessageEncoder = new MessageEncoder(
			senderKeyPair);

		// Encrypt the message using recipient's public key
		final byte[] secretMessage =
			"This is a secret message!".getBytes(UTF_8);
		final Message encryptedMessage = senderMessageEncoder.encode(
			recipientKeyPair.getPublicKey(), secretMessage
		);
		System.out.printf("Original message: %s%n",
			new String(secretMessage, UTF_8));
		System.out.printf("Encrypted payload: %s%n",
			HexFormat.of().formatHex(encryptedMessage.getMessage()));

		// Build transfer transaction with encrypted message
		final Transaction encryptedTransaction =
			facade.createTransactionFromTypedDescriptor(
				new TransferTransactionV2Descriptor(
					recipientAddress,
					new Amount(0),
					new MessageDescriptor(
						MessageType.ENCRYPTED,
						encryptedMessage.getMessage()),
					List.of()),
				senderKeyPair.getPublicKey(),
				0,
				2 * 60 * 60); // [<step-4]
		encryptedTransaction.setFee(new Amount(
			FeeCalculator.calculateTransactionFee(encryptedTransaction)));

		// Sign and announce the transaction
		final CryptoTypes.Signature encryptedSignature =
			facade.signTransaction(senderKeyPair, encryptedTransaction);
		final String encryptedJsonPayload = NemTransactionFactory
			.attachSignature(encryptedTransaction, encryptedSignature);
		final String encryptedTransactionHash = facade.hashTransaction(
			encryptedTransaction).toString();
		System.out.printf("Transaction hash: %s%n",
			encryptedTransactionHash);

		final HttpRequest encryptedRequest = HttpRequest.newBuilder(
			URI.create(nodeUrl + "/transaction/announce"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(
				encryptedJsonPayload))
			.build();
		HTTP_CLIENT.send(encryptedRequest, BodyHandlers.ofString());
		System.out.println("Encrypted message transaction announced\n");

		// --- RECEIVING ENCRYPTED MESSAGE ---
		System.out.println("<== Receiving Encrypted Message"); // [>step-5]

		// Wait for confirmation
		final JsonNode encryptedTxData = waitForConfirmation(
			encryptedTransactionHash, "Encrypted message transaction");

		// Decode encrypted message using recipient's private key
		final MessageEncoder recipientMessageEncoder = new MessageEncoder(
			recipientKeyPair);
		final Message receivedEncryptedMessage = new Message();
		receivedEncryptedMessage.setMessageType(MessageType.ENCRYPTED);
		receivedEncryptedMessage.setMessage(HexFormat.of().parseHex(
			encryptedTxData.get("transaction").get("message")
				.get("payload").asText()));

		// Get sender's public key from the transaction
		final CryptoTypes.PublicKey senderPublicKeyFromTx =
			new CryptoTypes.PublicKey(encryptedTxData.get("transaction")
				.get("signer").asText());

		final MessageEncoderResult result =
			recipientMessageEncoder.tryDecode(
				senderPublicKeyFromTx, receivedEncryptedMessage);

		if (result.isDecoded()) {
			System.out.printf("Recipient decrypted message: %s%n",
				new String((byte[]) result.message(), UTF_8));
		} else {
			System.out.println("Recipient failed to decrypt message");
		} // [<step-5]
	}
}

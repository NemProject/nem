package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.model.*;
import org.nem.nis.test.LocalHostConnector;
import org.nem.core.utils.*;

// this test requires node to be running on local host
//
// json objects are used directly (on purpose), to make it easier
// to follow/find/detect changes in /transfer/ API or serializers
public class TransferControllerTest {

	private static final String TRANSFER_PREPARE_PATH = "transaction/prepare";

	@Test
	public void transferIncorrectType() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = new JSONObject();
		obj.put("type", 123456);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		final byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectRecipient() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "AAAANBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		final byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(404));
	}

	@Test
	public void transferIncorrectAmount() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		final byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", -13L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectMessageType() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		final byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", 66);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectMessagePayload() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		final byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "dbcdefghijklmnopqrstuvwxyz)(*&^%$#@!");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectSigner() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		final byte[] signersKey = HexEncoder.getBytes("4202a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}

	@Test
	public void transferCorrectTransaction() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject obj = createValidTransaction();

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(200));
	}

	@Test
	public void transferFailsIfDeadlineIsInThePast() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject obj = createValidTransaction();
		obj.put("timestamp", 100);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
		Assert.assertThat(result.getError().getMessage(), IsEqual.equalTo("FAILURE_PAST_DEADLINE"));
	}

	private static JSONObject createValidTransaction() {
		final JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		final byte[] signersKey = HexEncoder.getBytes("02f25538f7fbdb0dbe7d3363be67f9edac8033c777cf3fc10ededae1a990c5459f");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);
		return obj;
	}

	private static String getRecipientAccountId() {
		return NetworkInfo.getDefault().getNemesisRecipientAccountIds()[2];
	}
}

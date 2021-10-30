package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.model.*;
import org.nem.nis.test.LocalHostConnector;

// this test requires node to be running on local host
//
// json objects are used directly (on purpose), to make it easier
// to follow/find/detect changes in /transfer/ API or serializers
public class TransferControllerITCase {
	private static final String TRANSFER_PREPARE_PATH = "transaction/prepare";
	private static final String RECIPIENT_ADDRESS = AcceptanceTestConstants.ADDRESS2.toString();
	private static final String SENDER_PUBLIC_KEY = AcceptanceTestConstants.PUBLIC_KEY.toString();

	@Test
	public void transferIncorrectType() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = createValidTransaction();
		obj.put("type", 123456);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(400));
		MatcherAssert.assertThat(result.getError().getMessage(), IsEqual.equalTo("Unknown transaction type: 123456"));
	}

	@Test
	public void transferIncorrectRecipient() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = createValidTransaction();
		obj.put("recipient", "bad recipient");

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(404));
		MatcherAssert.assertThat(result.getError().getMessage(), IsEqual.equalTo("invalid address 'BAD RECIPIENT' (org.nem.core.model.Address)"));
	}

	@Test
	public void transferIncorrectAmount() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = createValidTransaction();
		obj.put("amount", -13L);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(400));
		MatcherAssert.assertThat(result.getError().getMessage(), IsEqual.equalTo("amount (-13) must be non-negative"));
	}

	@Test
	public void transferIncorrectMessageType() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = createValidTransaction();
		final JSONObject message = new JSONObject();
		message.put("type", 66);
		message.put("payload", "48656C6C6F2C20576F726C6421");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(400));
		MatcherAssert.assertThat(result.getError().getMessage(), IsEqual.equalTo("Unknown message type: 66"));
	}

	@Test
	public void transferIncorrectMessagePayload() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = createValidTransaction();
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "48656C6C6F2G20576F726C6421");
		obj.put("message", message);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(400));
		MatcherAssert.assertThat(
				result.getError().getMessage(),
				IsEqual.equalTo("org.apache.commons.codec.DecoderException: Illegal hexadecimal character G at index 11"));
	}

	@Test
	public void transferIncorrectSigner() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = createValidTransaction();
		obj.put("signer", "bad signer");

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(400));
		MatcherAssert.assertThat(
				result.getError().getMessage(),
				IsEqual.equalTo("org.apache.commons.codec.DecoderException: Illegal hexadecimal character   at index 3"));
	}

	@Test
	public void transferCorrectTransaction() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject obj = createValidTransaction();

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(200));
		MatcherAssert.assertThat(result.hasBody(), IsEqual.equalTo(true));
	}

	@Test
	public void transferFailsIfDeadlineIsInThePast() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final JSONObject obj = createValidTransaction();
		obj.put("timeStamp", 100);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(TRANSFER_PREPARE_PATH, obj);

		// Assert:
		MatcherAssert.assertThat(result.getStatus(), IsEqual.equalTo(400));
		MatcherAssert.assertThat(result.getError().getMessage(), IsEqual.equalTo("FAILURE_PAST_DEADLINE"));
	}

	private static JSONObject createValidTransaction() {
		final JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 0x98000001);
		obj.put("recipient", RECIPIENT_ADDRESS);
		obj.put("signer", SENDER_PUBLIC_KEY);
		obj.put("amount", 42000000L);
		obj.put("fee", 4000000L);
		obj.put("timeStamp", 0);
		obj.put("deadline", 1);
		final JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "48656C6C6F2C20576F726C6421");
		obj.put("message", message);
		return obj;
	}
}

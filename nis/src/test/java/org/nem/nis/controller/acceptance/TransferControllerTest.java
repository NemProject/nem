package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.apache.commons.codec.DecoderException;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.*;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.nis.test.MockPeerConnector;
import org.nem.core.utils.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

// this test requires node to be running on local host
//
// json objects are used directly (on purpose), to make it easier
// to follow/find/detect changes in /transfer/ API or serializers
public class TransferControllerTest {

	@Test
	public void transferIncorrectType() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", 123456);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectRecipient() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "AAAANBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(404));
	}

	@Test
	public void transferIncorrectAmount() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", -13L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectMessageType() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", 66);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectMessagePayload() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "dbcdefghijklmnopqrstuvwxyz)(*&^%$#@!");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(400));
	}

	@Test
	public void transferIncorrectSigner() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		byte[] signersKey = HexEncoder.getBytes("4202a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(400));
	}

	@Test
	public void transferCorrectTransaction() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", getRecipientAccountId());
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", 42L);
		obj.put("fee", 1L);
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("status"), IsEqual.equalTo(null));
	}

	private static String getRecipientAccountId() {
		return NetworkInfo.getDefault().getGenesisRecipientAccountIds()[2];
	}
}

package org.nem.core.nis.controller;

import net.minidev.json.JSONObject;
import org.apache.commons.codec.DecoderException;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nem.core.model.MessageTypes;
import org.nem.core.model.TransactionTypes;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.test.MockPeerConnector;
import org.nem.core.utils.Base64Encoder;
import org.nem.core.utils.HexEncoder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
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
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", Long.valueOf(42));
		obj.put("fee", Long.valueOf(1));
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.notNullValue());
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
		obj.put("amount", Long.valueOf(42));
		obj.put("fee", Long.valueOf(1));
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.notNullValue());
	}

	@Test
	 public void transferIncorrectAmount() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", Long.valueOf(-13));
		obj.put("fee", Long.valueOf(1));
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.notNullValue());
	}

	@Test
	 public void transferIncorrectMessageType() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", Long.valueOf(42));
		obj.put("fee", Long.valueOf(1));
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", 66);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.notNullValue());
	}

	@Test
	public void transferIncorrectMessagePayload() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", Long.valueOf(42));
		obj.put("fee", Long.valueOf(1));
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "dbcdefghijklmnopqrstuvwxyz)(*&^%$#@!");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.notNullValue());
	}

	@Test
	public void transferIncorrectSigner() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		byte[] signersKey = HexEncoder.getBytes("4202a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", Long.valueOf(42));
		obj.put("fee", Long.valueOf(1));
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.notNullValue());
	}

	@Test
	public void transferCorrectTransaction() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException, DecoderException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		byte[] signersKey = HexEncoder.getBytes("02a7ac11bd1163850985f9db69ea23d536f568670bf55ba2e7c9e596260e6dbdfb");
		obj.put("signer", Base64Encoder.getString(signersKey));
		obj.put("amount", Long.valueOf(42));
		obj.put("fee", Long.valueOf(1));
		obj.put("timestamp", 0);
		obj.put("deadline", 1);
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JsonDeserializer res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.nullValue());
	}
}

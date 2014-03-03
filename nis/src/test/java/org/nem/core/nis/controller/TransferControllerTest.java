package org.nem.core.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nem.core.model.MessageTypes;
import org.nem.core.model.TransactionTypes;
import org.nem.core.test.MockPeerConnector;

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
	public void transferIncorrectType() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", 123456);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("signer", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
	}

	@Test
	public void transferIncorrectRecipient() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "AAAANBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("signer", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
	}

	@Test
	 public void transferIncorrectAmount() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("signer", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(-13));
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
	}

	@Test
	 public void transferIncorrectMessageType() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("signer", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		JSONObject message = new JSONObject();
		message.put("type", 66);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
	}

	@Test
	public void transferIncorrectMessagePayload() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("signer", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "dbcdefghijklmnopqrstuvwxyz)(*&^%$#@!");
		obj.put("message", message);

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
	}

	@Test
	public void transferIncorrectSigner() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("signer", "x03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
	}

	@Test
	public void transferCorrectTransaction() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", TransactionTypes.TRANSFER);
		obj.put("version", 1);
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("signer", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		JSONObject message = new JSONObject();
		message.put("type", MessageTypes.PLAIN);
		message.put("payload", "SGVsbG8sIFdvcmxkIQ==");
		obj.put("message", message);

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.nullValue());
	}
}

package org.nem.core.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.test.MockPeerConnector;
import org.nem.peer.PeerNetwork;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TransferControllerTest {

	// this test will fail right now because condition in ctor in VerifiableEntity
	@Test
	public void transferIncorrectRecipient() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("recipient", "___NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("sender", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		obj.put("message", "48656c6c6f20576f726c64");

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
		Assert.assertThat((Integer)res.get("error"), IsEqual.equalTo(2));
	}

	@Test
	 public void transferIncorrectAmount() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("sender", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(-13));
		obj.put("message", "48656c6c6f20576f726c64");

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
		Assert.assertThat((Integer)res.get("error"), IsEqual.equalTo(3));
	}

	@Test
	public void transferIncorrectMessage() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("sender", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		obj.put("message", "HelloWorld");

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
		Assert.assertThat((Integer)res.get("error"), IsEqual.equalTo(4));
	}

	@Test
	public void transferIncorrectSender() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("sender", "x03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		obj.put("message", "48656c6c6f20576f726c64");

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.notNullValue());
		Assert.assertThat((Integer)res.get("error"), IsEqual.equalTo(5));
	}


	// this test will also fail right now
	@Test
	public void transferCorrectTransaction() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("recipient", "NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		obj.put("sender", "03d671c0029ba81781be05702df62d05d7111be2223657c5b883794cb784e3c03c");
		obj.put("amount", Long.valueOf(42));
		obj.put("message", "48656c6c6f20576f726c64");

		// Act:
		JSONObject res = pc.transferPrepare(obj);

		// Assert:
		Assert.assertThat(res.get("error"), IsNull.nullValue());
	}
}

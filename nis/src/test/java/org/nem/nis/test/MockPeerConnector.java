package org.nem.nis.test;

import net.minidev.json.JSONObject;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;
import org.nem.peer.net.HttpMethodClient;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * ugliness
 */
public class MockPeerConnector {
	private final URL baseURL;

	private final HttpMethodClient httpMethodClient;

	public MockPeerConnector() throws MalformedURLException {
		super();

		this.baseURL = new URL("http", "127.0.0.1", 7890, "/");
		this.httpMethodClient = new HttpMethodClient(new DeserializationContext(new MockAccountLookup()), 30);
	}

	public JsonDeserializer transferPrepare(final JSONObject transferPrepareData) throws MalformedURLException {
		return this.post("transfer/prepare", transferPrepareData);
	}

	public JsonDeserializer pushTransaction(final JSONObject transferData) throws MalformedURLException {
		return this.post("push/transaction", transferData);
	}

	public JsonDeserializer accountUnlock(final JSONObject input) throws MalformedURLException {
		return this.post("account/unlock", input);
	}

	public JsonDeserializer blockAt(final JSONObject input) throws MalformedURLException {
		return this.post("block/at", input);
	}

	public JsonDeserializer wrongUrl(final JSONObject input) throws MalformedURLException {
		return this.post("wrong/at", input);
	}

	private JsonDeserializer post(final String path, final JSONObject requestData) throws MalformedURLException {
		return this.httpMethodClient.post(new URL(this.baseURL, path), requestData);
	}
}

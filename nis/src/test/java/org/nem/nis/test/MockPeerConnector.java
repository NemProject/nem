package org.nem.nis.test;

import net.minidev.json.JSONObject;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.peer.net.HttpMethodClient;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * ugliness
 */
public class MockPeerConnector {
	private URL baseURL;

	private final HttpMethodClient httpMethodClient;

	public MockPeerConnector() throws MalformedURLException {
		super();

		this.baseURL = new URL("http", "127.0.0.1", 7890, "/");
		this.httpMethodClient = new HttpMethodClient(null, 30);
	}

	public JsonDeserializer transferPrepare(final JSONObject transferPrepareData) throws MalformedURLException {
		return this.post("transfer/prepare", transferPrepareData);
	}

	public JsonDeserializer pushTransaction(final JSONObject transferData) throws MalformedURLException {
		return this.post("push/transaction", transferData);
	}

	private JsonDeserializer post(final String path, final JSONObject requestData) throws MalformedURLException {
		return this.httpMethodClient.post(new URL(this.baseURL, path), requestData);
	}
}

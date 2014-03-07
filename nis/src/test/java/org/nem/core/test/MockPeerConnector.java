package org.nem.core.test;

import net.minidev.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * ugliness
 */
public class MockPeerConnector {
	private URL baseURL;

	public MockPeerConnector() throws MalformedURLException {
		super();

		this.baseURL = new URL("http", "127.0.0.1", 7890, "/");
	}

	public JSONObject transferPrepare(JSONObject transferPrepareData) throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException, URISyntaxException {
//		JSONObject response = postResponse(new URL(this.baseURL, "transfer/prepare"), transferPrepareData);
//		return response;
        // TODO: fix this
        return new JSONObject();
	}
}

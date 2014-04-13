package org.nem.nis.test;

import net.minidev.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A mock HttpInputMessage implementation.
 */
public class MockHttpInputMessage implements HttpInputMessage {

	private final HttpHeaders headers;
	private final ByteArrayInputStream bodyStream;

	/**
	 * Creates a new mock HttpInputMessage.
	 *
	 * @param body The input message's body.
	 */
	public MockHttpInputMessage(final String body) {
		this.bodyStream = new ByteArrayInputStream(body.getBytes());
		this.headers = new HttpHeaders();
	}

	/**
	 * Creates a new mock HttpInputMessage.
	 *
	 * @param body The input message's body.
	 */
	public MockHttpInputMessage(final JSONObject body) {
		this(body.toJSONString());
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.bodyStream;
	}
}

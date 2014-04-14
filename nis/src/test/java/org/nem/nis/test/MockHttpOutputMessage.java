package org.nem.nis.test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A mock HttpOutputMessage implementation.
 */
public class MockHttpOutputMessage implements HttpOutputMessage {

	private final HttpHeaders headers = new HttpHeaders();
	private final ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.bodyStream;
	}

	/**
	 * Gets the body as a string.
	 *
	 * @return The body as a string.
	 */
	public String getBodyAsString() {
		return this.bodyStream.toString();
	}
}

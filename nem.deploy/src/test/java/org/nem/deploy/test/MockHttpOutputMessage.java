package org.nem.deploy.test;

import org.nem.core.utils.ExceptionUtils;
import org.springframework.http.*;

import java.io.*;

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
		return ExceptionUtils.propagate(() -> this.bodyStream.toString("UTF-8"));
	}
}

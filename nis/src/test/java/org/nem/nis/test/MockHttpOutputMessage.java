package org.nem.nis.test;

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
		String result = null; 
		try {
			result = this.bodyStream.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
}

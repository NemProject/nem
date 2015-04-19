package org.nem.core.connect;

import net.minidev.json.JSONObject;
import org.nem.core.serialization.*;

/**
 * A union that will either contain an error response or a deserializer.
 */
public class ErrorResponseDeserializerUnion {
	private final static int HTTP_OK = 200;

	private final int status;
	private final Object body;
	private final DeserializationContext context;

	/**
	 * Creates a new union.
	 *
	 * @param status The http status code.
	 * @param body The http response body.
	 * @param context The deserialization context.
	 */
	public ErrorResponseDeserializerUnion(
			final int status,
			final Object body,
			final DeserializationContext context) {
		this.status = status;
		this.body = body;
		this.context = context;
	}

	/**
	 * Gets the HTTP status.
	 *
	 * @return The http status.
	 */
	public int getStatus() {
		return this.status;
	}

	/**
	 * Gets a value indicating whether or not the current instance contains an error.
	 *
	 * @return true if this contains an error.
	 */
	public boolean hasError() {
		return HTTP_OK != this.status;
	}

	/**
	 * Gets a value indicating whether or not the current instance has a body.
	 *
	 * @return true if this instance has a body.
	 */
	public boolean hasBody() {
		return !(this.body instanceof String) || !((String)this.body).isEmpty();
	}

	/**
	 * Gets the response body as an ErrorResponse.
	 *
	 * @return The response body as an ErrorResponse.
	 */
	public ErrorResponse getError() {
		if (!this.hasError()) {
			throw new IllegalStateException("cannot retrieve error when there is no error");
		}

		return new ErrorResponse(this.getDeserializerUnchecked());
	}

	/**
	 * Gets the response body as a Deserializer.
	 *
	 * @return The response body as a Deserializer.
	 */
	public Deserializer getDeserializer() {
		if (this.hasError()) {
			throw new IllegalStateException("cannot retrieve deserializer when an error has occurred");
		}

		return this.getDeserializerUnchecked();
	}

	private Deserializer getDeserializerUnchecked() {
		if (!this.hasBody() || !(this.body instanceof JSONObject)) {
			throw new IllegalStateException("body must be a JSONObject");
		}

		return new JsonDeserializer((JSONObject)this.body, this.context);
	}
}

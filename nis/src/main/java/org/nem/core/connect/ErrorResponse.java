package org.nem.core.connect;

import org.nem.core.serialization.*;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeProvider;
import org.springframework.http.HttpStatus;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Response that is returned when an action fails.
 */
public class ErrorResponse implements SerializableEntity {

	private static final Logger LOGGER = Logger.getLogger(ErrorResponse.class.getName());
	private static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

	private final int timeStamp;
	private final String error;
	private final int status;
	private final String message;

	/**
	 * Creates a new error response.
	 *
	 * @param e The original exception.
	 * @param status The desired HttpStatus.
	 */
	public ErrorResponse(final Exception e, final HttpStatus status) {
		this(e.getMessage(), status.value());
	}

	/**
	 * Creates a new error response.
	 *
	 * @param message The error message.
	 * @param status The desired HTTP status code.
	 */
	public ErrorResponse(final String message, final int status) {
		this.status = status;
		this.error = getStatusReason(status);
		this.message = message;
		this.timeStamp = TIME_PROVIDER.getCurrentTime().getRawTime();

		final Level logLevel = this.status >= 500 ? Level.SEVERE : Level.INFO;
		LOGGER.log(logLevel, this.toString());
	}

	/**
	 * Deserializes an error response.
	 *
	 * @param deserializer The deserializer.
	 */
	public ErrorResponse(final Deserializer deserializer) {
		this.status = deserializer.readInt("status");
		this.error = deserializer.readOptionalString("error");
		this.message = deserializer.readOptionalString("message");
		this.timeStamp = deserializer.readInt("timeStamp");
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return The timestamp.
	 */
	public int getTimeStamp() { return this.timeStamp; }

	/**
	 * Gets the error reason phrase.
	 *
	 * @return The error reason phrase.
	 */
	public String getError() { return this.error; }

	/**
	 * Gets the error message.
	 *
	 * @return The error message.
	 */
	public String getMessage() { return this.message; }

	/**
	 * Gets the raw HTTP status.
	 *
	 * @return The raw HTTP status.
	 */
	public int getStatus() { return this.status; }

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("status", this.status);
		serializer.writeString("error", this.error);
		serializer.writeString("message", this.message);
		serializer.writeInt("timeStamp", this.timeStamp);
	}

	private static String getStatusReason(final int rawStatus) {
		try {
			final HttpStatus status = HttpStatus.valueOf(rawStatus);
			return status.getReasonPhrase();
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Http Status Code ");
		builder.append(this.status);

		final String message = null != this.message ? this.message : this.error;
		if (null != message) {
			builder.append(": ");
			builder.append(message);
		}

		return builder.toString();
	}
}

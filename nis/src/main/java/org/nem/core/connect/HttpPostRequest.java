package org.nem.core.connect;

/**
 * An interface specifying information about an HttpPost request.
 */
public interface HttpPostRequest {

	/**
	 * Gets the payload.
	 *
	 * @return The payload.
	 */
	public byte[] getPayload();

	/**
	 * Gets the content type.
	 *
	 * @return The content type.
	 */
	public String getContentType();
}

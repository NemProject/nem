package org.nem.core.serialization;

/**
 * Class that contains external state necessary for serialization of some objects.
 */
public class SerializationContext {

	/**
	 * Gets the maximum number of bytes that can be serialized.
	 *
	 * @return The maximum number of bytes.
	 */
	public int getDefaultMaxBytesLimit() {
		return 1024;
	}

	/**
	 * Gets the default maximum number of characters that can be serialized.
	 *
	 * @return The maximum number of characters.
	 */
	public int getDefaultMaxCharsLimit() {
		return 128;
	}
}

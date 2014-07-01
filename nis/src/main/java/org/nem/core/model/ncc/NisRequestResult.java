package org.nem.core.model.ncc;

import org.nem.core.serialization.*;

/**
 * 
 * Result for NIS request from NCC.
 *
 */
public class NisRequestResult implements SerializableEntity {
	/**
	 * Result type: validation result 
	 */
	public static final int TYPE_VALIDATION_RESULT = 0x00000001;
	
	/**
	 * Special code: neutral. 
	 */
	public static final int CODE_NEUTRAL = 0x00000000;
	
	/**
	 * Special code: success. 
	 */
	public static final int CODE_SUCCESS = 0x00000001;
	
	/**
	 * The result type
	 */
	private final int type;
	
	/**
	 * The result code
	 */
	private final int code;
	
	/**
	 * The result message
	 */
	private final String message;
	
	/**
	 * Creates a Nis request result.
	 * 
	 * @param code The type of the request result.
	 * @param code The code of the request result.
	 * @param code The message of the request result.
	 */
	public NisRequestResult(final int type, final int code, final String message) {
		this.type = type;
		this.code = code;
		this.message = message;
	}

	/**
	 * Deserializes a Nis request result.
	 *
	 * @param deserializer The deserializer.
	 */
	public NisRequestResult(final Deserializer deserializer) {
		this(deserializer.readInt("type"), 
			 deserializer.readInt("code"), 
			 deserializer.readString("message"));
	}
	
	/**
	 * Gets the type for the request result.
	 * @return The type for the request result.
	 */
	public int getType() {
		return this.type;
	}
	
	/**
	 * Gets the code for the request result.
	 * @return The code for the request result.
	 */
	public int getCode() {
		return this.code;
	}
	
	/**
	 * Gets the message for the request result.
	 * @return The code for the request result.
	 */
	public String getMessage() {
		return this.message;
	}
	
	/**
	 * Indicating whether or not the the result represents an error
	 */
	public boolean isError() {
		return CODE_NEUTRAL != this.code &&
			   CODE_SUCCESS != this.code;
	}
	
	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("type", this.type);
		serializer.writeInt("code", this.code);
		serializer.writeString("message", this.message);
	}
}

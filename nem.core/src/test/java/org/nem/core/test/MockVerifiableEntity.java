package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * A mock VerifiableEntity implementation.
 */
public class MockVerifiableEntity extends VerifiableEntity {
	public static final int TYPE = 12;
	public static final int VERSION = 24;
	public static final TimeInstant TIMESTAMP = new TimeInstant(127435);

	private int customField;

	/**
	 * Creates a mock verifiable entity.
	 *
	 * @param signer The owner's account.
	 */
	public MockVerifiableEntity(final Account signer) {
		this(signer, 0);
	}

	/**
	 * Creates a mock verifiable entity.
	 *
	 * @param signer The owner's account.
	 * @param customField The initial custom field value.
	 */
	public MockVerifiableEntity(final Account signer, final int customField) {
		super(TYPE, VERSION, TIMESTAMP, signer);
		this.customField = customField;
	}

	/**
	 * Deserializes a mock verifiable entity.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public MockVerifiableEntity(final Deserializer deserializer) {
		this(DeserializationOptions.VERIFIABLE, deserializer);
	}

	/**
	 * Deserializes a mock verifiable entity.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public MockVerifiableEntity(final DeserializationOptions options, final Deserializer deserializer) {
		super(deserializer.readInt("type"), options, deserializer);
		this.customField = deserializer.readInt("customField");
	}

	/**
	 * Gets the custom field value.
	 *
	 * @return The custom field value.
	 */
	public int getCustomField() {
		return this.customField;
	}

	/**
	 * Sets the custom field value.
	 *
	 * @param customField The desired custom field value.
	 */
	public void setCustomField(final int customField) {
		this.customField = customField;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		serializer.writeInt("customField", this.customField);
	}
}
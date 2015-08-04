package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.utils.MustBe;

/**
 * Class defining a mosaic definition (immutable).
 */
public class MosaicDefinition implements SerializableEntity {
	private final Account creator;
	private final MosaicId id;
	private final MosaicDescriptor descriptor;
	private final MosaicProperties properties;
	private final MosaicTransferFeeInfo transferFeeInfo;

	/**
	 * Creates a new mosaic definition.
	 *
	 * @param creator The creator.
	 * @param id The id.
	 * @param descriptor The descriptor.
	 * @param properties The properties.
	 */
	public MosaicDefinition(
			final Account creator,
			final MosaicId id,
			final MosaicDescriptor descriptor,
			final MosaicProperties properties) {
		this(creator, id, descriptor, properties, null);
	}

	/**
	 * Creates a new mosaic definition.
	 *
	 * @param creator The creator.
	 * @param id The id.
	 * @param descriptor The descriptor.
	 * @param properties The properties.
	 */
	public MosaicDefinition(
			final Account creator,
			final MosaicId id,
			final MosaicDescriptor descriptor,
			final MosaicProperties properties,
			final MosaicTransferFeeInfo transferFeeInfo) {
		this.creator = creator;
		this.id = id;
		this.descriptor = descriptor;
		this.properties = properties;
		this.transferFeeInfo = transferFeeInfo;
		this.validateFields();
	}

	/**
	 * Deserializes a mosaic definition.
	 *
	 * @param deserializer The deserializer.
	 */
	public MosaicDefinition(final Deserializer deserializer) {
		this.creator = Account.readFrom(deserializer, "creator", AddressEncoding.PUBLIC_KEY);
		this.id = deserializer.readObject("id", MosaicId::new);
		this.descriptor = MosaicDescriptor.readFrom(deserializer, "description");
		this.properties = new DefaultMosaicProperties(deserializer.readObjectArray("properties", NemProperty::new));
		this.transferFeeInfo = deserializer.readOptionalObject("transferFeeInfo", MosaicTransferFeeInfo::new);
		this.validateFields();
	}

	private void validateFields() {
		MustBe.notNull(this.creator, "creator");
		MustBe.notNull(this.id, "id");
		MustBe.notNull(this.descriptor, "descriptor");
		MustBe.notNull(this.properties, "properties");
	}

	/**
	 * Gets the mosaic's creator.
	 *
	 * @return The creator.
	 */
	public Account getCreator() {
		return this.creator;
	}

	/**
	 * Gets the mosaic's id.
	 *
	 * @return The id.
	 */
	public MosaicId getId() {
		return this.id;
	}

	/**
	 * Gets the mosaic's descriptor.
	 *
	 * @return The descriptor.
	 */
	public MosaicDescriptor getDescriptor() {
		return this.descriptor;
	}

	/**
	 * Gets the mosaic's properties.
	 *
	 * @return The properties.
	 */
	public MosaicProperties getProperties() {
		return this.properties;
	}


	/**
	 * Gets the optional transfer fee info.
	 *
	 * @return The transfer fee info or null if not available.
	 */
	public MosaicTransferFeeInfo getTransferFeeInfo() {
		return this.transferFeeInfo;
	}

	@Override
	public void serialize(final Serializer serializer) {
		Account.writeTo(serializer, "creator", this.creator, AddressEncoding.PUBLIC_KEY);
		serializer.writeObject("id", this.id);
		MosaicDescriptor.writeTo(serializer, "description", this.descriptor);
		serializer.writeObjectArray("properties", this.properties.asCollection());
		serializer.writeObject("transferFeeInfo",  null == this.transferFeeInfo ? null : this.transferFeeInfo);
	}

	@Override
	public String toString() {
		return this.id.toString();
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MosaicDefinition)) {
			return false;
		}

		final MosaicDefinition rhs = (MosaicDefinition)obj;
		return this.id.equals(rhs.id);
	}
}

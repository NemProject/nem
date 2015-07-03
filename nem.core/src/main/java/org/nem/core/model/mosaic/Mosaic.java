package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.serialization.*;

import java.util.*;

/**
 * Class defining a mosaic.
 */
public class Mosaic implements SerializableEntity {
	private final Account creator;
	private final MosaicId id;
	private final MosaicDescriptor descriptor;
	private final NamespaceId namespaceId;
	private final GenericAmount amount;
	private final MosaicProperties properties;
	private final List<Mosaic> children;

	/**
	 * Creates a new mosaic.
	 *
	 * @param creator The creator.
	 * @param id The id.
	 * @param descriptor The descriptor.
	 * @param namespaceId The namespace id.
	 * @param amount The amount.
	 * @param properties The properties.
	 */
	public Mosaic(
			final Account creator,
			final MosaicId id,
			final MosaicDescriptor descriptor,
			final NamespaceId namespaceId,
			final GenericAmount amount,
			final MosaicProperties properties) {
		this.creator = creator;
		this.id = id;
		this.descriptor = descriptor;
		this.namespaceId = namespaceId;
		this.amount = amount;
		this.properties = properties;
		this.children = Collections.emptyList();
		this.validateFields();
	}

	/**
	 * Deserializes a mosaic.
	 *
	 * @param deserializer The deserializer.
	 */
	public Mosaic(final Deserializer deserializer) {
		this.creator = Account.readFrom(deserializer, "creator", AddressEncoding.PUBLIC_KEY);
		this.id = MosaicId.readFrom(deserializer, "id");
		this.descriptor = MosaicDescriptor.readFrom(deserializer, "description");
		this.namespaceId = NamespaceId.readFrom(deserializer, "namespaceId");
		this.amount = GenericAmount.readFrom(deserializer, "amount");
		this.properties = new MosaicPropertiesImpl(deserializer.readObjectArray("properties", NemProperty::new));
		this.children = deserializer.readObjectArray("children", Mosaic::new);
		this.validateFields();
	}

	private void validateFields() {
		if (null == this.creator) {
			throw new IllegalArgumentException("creator of the mosaic cannot be null");
		}

		if (null == this.id) {
			throw new IllegalArgumentException("id of the mosaic cannot be null");
		}

		if (null == this.descriptor) {
			throw new IllegalArgumentException("descriptor of the mosaic cannot be null");
		}

		if (null == this.namespaceId) {
			throw new IllegalArgumentException("namespace id of the mosaic cannot be null");
		}

		if (null == this.amount || GenericAmount.ZERO.equals(amount)) {
			throw new IllegalArgumentException("amount of the mosaic cannot be null and must be larger than zero");
		}

		if (null == this.properties) {
			throw new IllegalArgumentException("properties of the mosaic cannot be null");
		}

		if (!this.children.isEmpty()) {
			throw new IllegalArgumentException("nesting of mosaics not allowed in stage 1 implementation");
		}
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
	 * Gets the underlying namespace id.
	 *
	 * @return The namespace id.
	 */
	public NamespaceId getNamespaceId() {
		return this.namespaceId;
	}

	/**
	 * Gets the mosaic's amount.
	 *
	 * @return The amount.
	 */
	public GenericAmount getAmount() {
		return this.amount;
	}

	// TODO 20150702 J-B: the following looks like effectively an implementation of MosaicProperties
	// > any reason not to have a getProperties() { return properties }

	// TODO 20150702 J-B: i also think we should make a distinction between required properties (name, desc, namespace)
	// > and optional properties (everything else)
	// > i mean those three can probably be in a separate object like MosaicId or MosaicDescriptor
	// TODO 20150703 BR -> J: makes sense.

	/**
	 * Gets the properties as a collection.
	 *
	 * @return The collection of nem properties.
	 */
	public MosaicProperties getProperties() {
		return this.properties;
	}

	/**
	 * Gets the mosaic's children.
	 *
	 * @return The children.
	 */
	public Collection<Mosaic> getChildren() {
		return this.children;
	}

	@Override
	public void serialize(final Serializer serializer) {
		Account.writeTo(serializer, "creator", this.creator, AddressEncoding.PUBLIC_KEY);
		MosaicId.writeTo(serializer, "id", this.id);
		MosaicDescriptor.writeTo(serializer, "description", this.descriptor);
		NamespaceId.writeTo(serializer, "namespaceId", this.namespaceId);
		GenericAmount.writeTo(serializer, "amount", this.amount);
		serializer.writeObjectArray("properties", this.properties.asCollection());
		// TODO 20150702: assuming that the children are references to other objects, writing the ids is probably good enough
		// TODO 20150703 BR -> J: how do you imagine to deserialize them?
		serializer.writeObjectArray("children", this.children);
	}
}

package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Represents a multisig minimum cosignatories modification.
 */
public class MultisigMinCosignatoriesModification implements SerializableEntity {
	private final MultisigModificationType modificationType;
	private final int minCosignatories;

	/**
	 * Creates a multisig minimum cosignatories modification.
	 *
	 * @param modificationType The modification type.
	 * @param minCosignatories The minimum number of cosignatories.
	 */
	public MultisigMinCosignatoriesModification(final MultisigModificationType modificationType, final int minCosignatories) {
		this.modificationType = modificationType;
		this.minCosignatories = minCosignatories;

		this.validate();
	}

	/**
	 * Deserializes a multisig minimum cosignatories modification.
	 *
	 * @param deserializer The deserializer.
	 */
	public MultisigMinCosignatoriesModification(final Deserializer deserializer) {
		this.modificationType = MultisigModificationType.fromValueOrDefault(deserializer.readInt("modificationType"));
		this.minCosignatories = deserializer.readInt("minCosignatories");

		this.validate();
	}

	/**
	 * Gets the minimum number of cosignatories.
	 *
	 * @return The minimum number of cosignatories.
	 */
	public int getMinCosignatories() {
		return this.minCosignatories;
	}

	/**
	 * Gets the modification type.
	 *
	 * @return The modification type.
	 */
	public MultisigModificationType getModificationType() {
		return this.modificationType;
	}

	private void validate() {
		if (0 > this.minCosignatories) {
			throw new IllegalArgumentException("minimum number of cosignatories cannot be negative");
		}

		if (!MultisigModificationType.MinCosignatories.equals(this.modificationType)) {
			throw new IllegalArgumentException(String.format("invalid modification type: %s", this.modificationType));
		}
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("modificationType", this.modificationType.value());
		serializer.writeInt("minCosignatories", this.minCosignatories);
	}
}

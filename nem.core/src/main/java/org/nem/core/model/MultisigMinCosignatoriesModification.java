package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Represents a multisig minimum cosignatories modification.
 */
public class MultisigMinCosignatoriesModification implements SerializableEntity {
	private final int minCosignatories;

	/**
	 * Creates a multisig minimum cosignatories modification.
	 *
	 * @param minCosignatories The minimum number of cosignatories.
	 */
	public MultisigMinCosignatoriesModification(final int minCosignatories) {
		this.minCosignatories = minCosignatories;

		this.validate();
	}

	/**
	 * Deserializes a multisig minimum cosignatories modification.
	 *
	 * @param deserializer The deserializer.
	 */
	public MultisigMinCosignatoriesModification(final Deserializer deserializer) {
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

	private void validate() {
		if (0 >= this.minCosignatories) {
			throw new IllegalArgumentException("minimum number of cosignatories must be positive");
		}
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("minCosignatories", this.minCosignatories);
	}
}

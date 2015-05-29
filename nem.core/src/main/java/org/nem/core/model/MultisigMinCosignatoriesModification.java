package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Represents a multisig minimum cosignatories modification.
 */
public class MultisigMinCosignatoriesModification implements SerializableEntity {
	private final int relativeChange;

	/**
	 * Creates a multisig minimum cosignatories modification.
	 *
	 * @param relativeChange The minimum number of cosignatories.
	 */
	public MultisigMinCosignatoriesModification(final int relativeChange) {
		this.relativeChange = relativeChange;

		this.validate();
	}

	/**
	 * Deserializes a multisig minimum cosignatories modification.
	 *
	 * @param deserializer The deserializer.
	 */
	public MultisigMinCosignatoriesModification(final Deserializer deserializer) {
		this.relativeChange = deserializer.readInt("relativeChange");

		this.validate();
	}

	/**
	 * Gets the minimum number of cosignatories.
	 *
	 * @return The minimum number of cosignatories.
	 */
	public int getRelativeChange() {
		return this.relativeChange;
	}

	private void validate() {
		if (0 == this.relativeChange) {
			throw new IllegalArgumentException("relative change of minimum cosignatories must be non-zero");
		}
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("relativeChange", this.relativeChange);
	}
}

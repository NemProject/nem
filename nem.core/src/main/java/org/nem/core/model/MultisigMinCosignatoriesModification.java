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
	}

	/**
	 * Deserializes a multisig minimum cosignatories modification.
	 *
	 * @param deserializer The deserializer.
	 */
	public MultisigMinCosignatoriesModification(final Deserializer deserializer) {
		this.relativeChange = deserializer.readInt("relativeChange");
	}

	/**
	 * Gets the minimum number of cosignatories.
	 *
	 * @return The minimum number of cosignatories.
	 */
	public int getRelativeChange() {
		return this.relativeChange;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("relativeChange", this.relativeChange);
	}
}

package org.nem.core.model.ncc;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Represents additional data about multisig account
 */
public class MultisigInfo implements SerializableEntity {
	private final int cosignatoriesCount;
	private final int minCosignatories;

	/**
	 * Creates new multisig info view model.
	 *
	 * @param cosignatoriesCount The number of cosignatories.
	 * @param minCosignatories Minimum number of required cosignatories to make a transaction.
	 */
	public MultisigInfo(final int cosignatoriesCount, final int minCosignatories) {
		this.cosignatoriesCount = cosignatoriesCount;
		this.minCosignatories = minCosignatories;
	}

	/**
	 * Deserializes a multisig info view model.
	 *
	 * @param deserializer The deserializer.
	 */
	public MultisigInfo(final Deserializer deserializer) {
		this.cosignatoriesCount = deserializer.readInt("cosignatoriesCount");
		this.minCosignatories = deserializer.readInt("minCosignatories");
	}

	@Override
	public void serialize(Serializer serializer) {
		serializer.writeInt("cosignatoriesCount", this.cosignatoriesCount);
		serializer.writeInt("minCosignatories", this.minCosignatories);
	}

	/**
	 * Gets the number of cosignatories.
	 *
	 * @return The number of cosignatories.
	 */
	public int getCosignatoriesCount() {
		return cosignatoriesCount;
	}

	/**
	 * Gets the minimum number of cosignatories required.
	 *
	 * @return The minimum number of cosignatories required.
	 */
	public int getMinCosignatories() {
		return minCosignatories;
	}
}

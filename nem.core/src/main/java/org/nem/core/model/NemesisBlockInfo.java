package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;

/**
 * Represents information about a nemesis block.
 */
public class NemesisBlockInfo {
	private final Hash generationHash;
	private final Address address;
	private final Amount amount;
	private final String dataFileName;

	/**
	 * Creates a new nemesis block info.
	 *
	 * @param generationHash The generation hash.
	 * @param address The nemesis harvester address.
	 * @param amount The amount.
	 * @param dataFileName The data file name.
	 */
	public NemesisBlockInfo(
			final Hash generationHash,
			final Address address,
			final Amount amount,
			final String dataFileName) {
		this.generationHash = generationHash;
		this.address = address;
		this.amount = amount;
		this.dataFileName = dataFileName;
	}

	/**
	 * Gets the generation hash.
	 *
	 * @return The generation hash.
	 */
	public Hash getGenerationHash() {
		return this.generationHash;
	}

	/**
	 * Gets the address.
	 *
	 * @return The address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the amount.
	 *
	 * @return The amount.
	 */
	public Amount getAmount() {
		return this.amount;
	}

	/**
	 * Gets the data file name.
	 *
	 * @return The data file name.
	 */
	public String getDataFileName() {
		return this.dataFileName;
	}
}
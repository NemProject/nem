package org.nem.core.model;

/**
 * Enum containing types of multisig modifications.
 */
public enum MultisigModificationType {
	/**
	 * An unknown mode.
	 */
	Unknown(0),

	/**
	 * When adding cosignatory to multisig account.
	 */
	Add(1);

	/**
	 * Removal summary:
	 * 1. Removal will only be allowed in multisig transactions
	 * 2. There can be only one Del per MultisigSignerModificationTransaction
	 * 3. There will be N-2 signatures required
	 * (1 that gets removed, and 1 for person issuing MultisigTransaction)
	 */
	//Del(2);

	private final int value;

	MultisigModificationType(final int value) {
		this.value = value;
	}

	public boolean isValid() {
		switch (this) {
			case Add:
				//case Del:
				return true;
		}

		return false;
	}

	/**
	 * Creates a mode given a raw value.
	 *
	 * @param value The value.
	 * @return The mode if the value is known or Unknown if it was not.
	 */
	public static MultisigModificationType fromValueOrDefault(final int value) {
		for (final MultisigModificationType modificationType : values()) {
			if (modificationType.value() == value) {
				return modificationType;
			}
		}

		return MultisigModificationType.Unknown;
	}

	/**
	 * Gets the underlying integer representation of the mode.
	 *
	 * @return The underlying value.
	 */
	public int value() {
		return this.value;
	}
}

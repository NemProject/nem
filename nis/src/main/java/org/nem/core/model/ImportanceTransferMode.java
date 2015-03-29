package org.nem.core.model;

/**
 * Enum containing modes of importance transfers
 */
public enum ImportanceTransferMode {

	/**
	 * An unknown mode.
	 */
	Unknown(0),

	/**
	 * When announcing importance transfer.
	 */
	Activate(1),

	/**
	 * When canceling association between account and importance transfer.
	 */
	Deactivate(2);

	private final int value;

	ImportanceTransferMode(final int value) {
		this.value = value;
	}

	public boolean isValid() {
		switch (this) {
			case Activate:
			case Deactivate:
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
	public static ImportanceTransferMode fromValueOrDefault(final int value) {
		for (final ImportanceTransferMode mode : values()) {
			if (mode.value == value) {
				return mode;
			}
		}

		return ImportanceTransferMode.Unknown;
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

package org.nem.core.model;

/**
 * Represents information about a network.
 */
public class NetworkInfo {
	private final byte version;
	private final char addressStartChar;
	private final NemesisBlockInfo nemesisBlockInfo;

	/**
	 * Creates a new network info.
	 *
	 * @param version The network version.
	 * @param addressStartChar The character with which all network addresses should start.
	 * @param nemesisBlockInfo Information about the network nemesis block.
	 */
	public NetworkInfo(
			final byte version,
			final char addressStartChar,
			final NemesisBlockInfo nemesisBlockInfo) {
		this.version = version;
		this.addressStartChar = addressStartChar;
		this.nemesisBlockInfo = nemesisBlockInfo;
	}

	/**
	 * Gets the network version.
	 *
	 * @return The network version.
	 */
	public byte getVersion() {
		return this.version;
	}

	/**
	 * Gets the character with which all network addresses should start.
	 *
	 * @return The character with which all network addresses should start.
	 */
	public char getAddressStartChar() {
		return this.addressStartChar;
	}

	/**
	 * Gets information about the network nemesis block.
	 *
	 * @return Information about the network nemesis block.
	 */
	public NemesisBlockInfo getNemesisBlockInfo() {
		return this.nemesisBlockInfo;
	}

	/**
	 * Gets a value indicating whether or not the specified address is compatible with the network.
	 *
	 * @param address The address.
	 * @return true if the address is compatible with the network.
	 */
	public boolean isCompatible(final Address address) {
		try {
			return this.version == address.getVersion();
		} catch (final IllegalArgumentException e) {
			return false;
		}
	}
}

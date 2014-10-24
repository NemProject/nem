package org.nem.core.model;

import org.nem.core.utils.Base32Encoder;

/**
 * Represents information about a current network.
 */
public class NetworkInfo {
	private static final NetworkInfo MAIN_NETWORK_INFO = createMainNetworkInfo();
	private static final NetworkInfo TEST_NETWORK_INFO = createTestNetworkInfo();
	private static final NetworkInfo[] KNOWN_NETWORKS = new NetworkInfo[] { MAIN_NETWORK_INFO, TEST_NETWORK_INFO };

	private byte version;
	private char addressStartChar;
	private String nemesisAccountId;

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
	 * Gets the network nemesis account.
	 *
	 * @return The network nemesis account.
	 */
	public String getNemesisAccountId() {
		return this.nemesisAccountId;
	}

	/**
	 * Gets a value indicating whether or not the specified address is compatible with the network.
	 *
	 * @param address The address.
	 * @return true if the address is compatible with the network.
	 */
	public boolean isCompatible(final Address address) {
		try {
			return this.version == getVersionFromAddress(address);
		} catch (final IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Gets information about the MAIN network.
	 *
	 * @return Information about the MAIN network.
	 */
	public static NetworkInfo getMainNetworkInfo() {
		return MAIN_NETWORK_INFO;
	}

	/**
	 * Gets information about the TEST network.
	 *
	 * @return Information about the TEST network.
	 */
	public static NetworkInfo getTestNetworkInfo() {
		return TEST_NETWORK_INFO;
	}

	/**
	 * Gets the network info from an address.
	 *
	 * @param address The address.
	 * @return The network info.
	 */
	public static NetworkInfo fromAddress(final Address address) {
		final byte version = getVersionFromAddress(address);
		for (final NetworkInfo info : KNOWN_NETWORKS) {
			if (version == info.getVersion()) {
				return info;
			}
		}

		throw new IllegalArgumentException(String.format("Invalid address '%s' is not part of any known network", address));
	}

	private static byte getVersionFromAddress(final Address address) {
		return Base32Encoder.getBytes(address.getEncoded())[0];
	}

	/**
	 * Gets information about the DEFAULT network.
	 *
	 * @return Information about the DEFAULT network.
	 */
	public static NetworkInfo getDefault() {
		return getTestNetworkInfo();
	}

	private static NetworkInfo createMainNetworkInfo() {
		final NetworkInfo info = new NetworkInfo();
		info.version = 0x68;
		info.addressStartChar = 'N';
		info.nemesisAccountId = "Not-a-real-address";
		return info;
	}

	private static NetworkInfo createTestNetworkInfo() {
		final NetworkInfo info = new NetworkInfo();
		info.version = (byte)0x98;
		info.addressStartChar = 'T';
		info.nemesisAccountId = "TANEMWISEEPBJ2BU5OZO6AUCSEHSZZBPTY5VNXRM";
		return info;
	}
}

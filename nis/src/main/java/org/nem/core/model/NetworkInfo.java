package org.nem.core.model;

import org.nem.core.utils.Base32Encoder;
/**
 * Represents information about a current network.
 */
public class NetworkInfo {
	private static final byte MAIN_NET_VERSION = (byte)0x68;
	private static final byte TEST_NET_VERSION = (byte)0x98;
	private static final char MAIN_NET_START_CHAR = 'N';
	private static final char TEST_NET_START_CHAR = 'T';
	private static final String MAIN_NET_NEMESIS_ACCOUNT_ID = "Not-a-real-address";
	private static final String TEST_NET_NEMESIS_ACCOUNT_ID = "TANEMWISEEPBJ2BU5OZO6AUCSEHSZZBPTY5VNXRM";
	private static final NetworkInfo mainNetworkInfo = createMainNetworkInfo();
	private static final NetworkInfo testNetworkInfo = createTestNetworkInfo();

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
	 * Gets information about the MAIN network.
	 *
	 * @return Information about the MAIN network.
	 */
	public static NetworkInfo getMainNetworkInfo() {
		return mainNetworkInfo;
	}

	/**
	 * Gets information about the TEST network.
	 *
	 * @return Information about the TEST network.
	 */
	public static NetworkInfo getTestNetworkInfo() {
		return testNetworkInfo;
	}

	/**
	 * Gets the network info from an address.
	 *
	 * @param address The address.
	 * @return The network info.
	 */
	public static NetworkInfo fromAddress(final Address address) {
		final byte[] encodedBytes;
		try {
			encodedBytes = Base32Encoder.getBytes(address.getEncoded());
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid address.", e);
		}

		switch (encodedBytes[0]) {
			case MAIN_NET_VERSION:
				return createMainNetworkInfo();
			case TEST_NET_VERSION:
				return createTestNetworkInfo();
			default:
				throw new IllegalArgumentException("Invalid address");
		}
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
		info.version = MAIN_NET_VERSION;
		info.addressStartChar = MAIN_NET_START_CHAR;
		info.nemesisAccountId = MAIN_NET_NEMESIS_ACCOUNT_ID;
		return info;
	}

	private static NetworkInfo createTestNetworkInfo() {
		final NetworkInfo info = new NetworkInfo();
		info.version = TEST_NET_VERSION;
		info.addressStartChar = TEST_NET_START_CHAR;
		info.nemesisAccountId = TEST_NET_NEMESIS_ACCOUNT_ID;
		return info;
	}
}

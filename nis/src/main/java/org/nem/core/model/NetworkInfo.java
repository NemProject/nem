package org.nem.core.model;

/**
 * Represents information about a current network.
 */
public class NetworkInfo {
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

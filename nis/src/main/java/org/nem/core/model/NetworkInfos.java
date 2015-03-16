package org.nem.core.model;

/**
 * Central class responsible for providing access to network information.
 */
public class NetworkInfos {
	private static final NetworkInfo MAIN_NETWORK_INFO = createMainNetworkInfo();
	private static final NetworkInfo TEST_NETWORK_INFO = createTestNetworkInfo();
	private static final NetworkInfo[] KNOWN_NETWORKS = new NetworkInfo[] { MAIN_NETWORK_INFO, TEST_NETWORK_INFO };
	private static NetworkInfo DEFAULT_NETWORK_INFO;

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
		return fromVersion(address.getVersion());
	}

	/**
	 * Gets the network info from the version.
	 *
	 * @param version The version.
	 * @return The network info.
	 */
	public static NetworkInfo fromVersion(final byte version) {
		for (final NetworkInfo info : KNOWN_NETWORKS) {
			if (version == info.getVersion()) {
				return info;
			}
		}

		throw new IllegalArgumentException(String.format("Invalid version '%d' is not a known network version", version));
	}

	/**
	 * Gets information about the DEFAULT network.
	 *
	 * @return Information about the DEFAULT network.
	 */
	public static NetworkInfo getDefault() {
		return null == DEFAULT_NETWORK_INFO ? getTestNetworkInfo() : DEFAULT_NETWORK_INFO;
	}

	/**
	 * Sets the default network.
	 *
	 * @param networkInfo The default network info.
	 */
	public static void setDefault(final NetworkInfo networkInfo) {
		if (null != DEFAULT_NETWORK_INFO && null != networkInfo) {
			throw new IllegalStateException("cannot change default network");
		}

		DEFAULT_NETWORK_INFO = networkInfo;
	}


	private static NetworkInfo createMainNetworkInfo() {
		return new NetworkInfo(
				(byte)0x68,
				'N',
				"Not-a-real-address");
	}

	private static NetworkInfo createTestNetworkInfo() {
		return new NetworkInfo(
				(byte)0x98,
				'T',
				"TBULEAUG2CZQISUR442HWA6UAKGWIXHDABJVIPS4");
	}
}

package org.nem.core.model;

/**
 * Central class responsible for providing access to network information.
 */
public class NetworkInfos {
	private static final NetworkInfo MAIN_NETWORK_INFO = createMainNetworkInfo();
	private static final NetworkInfo TEST_NETWORK_INFO = createTestNetworkInfo();
	private static final NetworkInfo[] KNOWN_NETWORKS = new NetworkInfo[] { MAIN_NETWORK_INFO, TEST_NETWORK_INFO };

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
		final byte version = address.getVersion();
		for (final NetworkInfo info : KNOWN_NETWORKS) {
			if (version == info.getVersion()) {
				return info;
			}
		}

		throw new IllegalArgumentException(String.format("Invalid address '%s' is not part of any known network", address));
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

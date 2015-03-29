package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.model.primitive.Amount;

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
		final byte version = (byte)0x68;
		final Address nemesisAddress = Address.fromPublicKey(
				version,
				PublicKey.fromHexString("8d07f90fb4bbe7715fa327c926770166a11be2e494a970605f2e12557f66c9b9"));
		return new NetworkInfo(
				(byte)0x68,
				'N',
				new NemesisBlockInfo(
						Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4"),
						nemesisAddress,
						Amount.fromNem(9000000000L + 240L),
						"nemesis.bin"));
	}

	private static NetworkInfo createTestNetworkInfo() {
		final byte version = (byte)0x98;
		final Address nemesisAddress = Address.fromPublicKey(
				version,
				PublicKey.fromHexString("e59ef184a612d4c3c4d89b5950eb57262c69862b2f96e59c5043bf41765c482f"));
		return new NetworkInfo(
				(byte)0x98,
				'T',
				new NemesisBlockInfo(
						Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4"),
						nemesisAddress,
						Amount.fromNem(8000000000L),
						"nemesis-testnet.bin"));
	}
}

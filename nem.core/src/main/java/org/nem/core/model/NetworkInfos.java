package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.core.model.primitive.Amount;
import org.nem.core.utils.SetOnce;

/**
 * Central class responsible for providing access to network information.
 */
public class NetworkInfos {
	private static final NetworkInfo MAIN_NETWORK_INFO = createMainNetworkInfo();
	private static final NetworkInfo TEST_NETWORK_INFO = createTestNetworkInfo();
	private static final NetworkInfo MIJIN_NETWORK_INFO = createMijinNetworkInfo();
	private static final NetworkInfo[] KNOWN_NETWORKS = new NetworkInfo[] { MAIN_NETWORK_INFO, TEST_NETWORK_INFO, MIJIN_NETWORK_INFO };
	private static final SetOnce<NetworkInfo> NETWORK_INFO = new SetOnce<>(TEST_NETWORK_INFO);

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
	 * Gets information about the MIJIN network.
	 *
	 * @return Information about the MIJIN network.
	 */
	public static NetworkInfo getMijinNetworkInfo() {
		return MIJIN_NETWORK_INFO;
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
	 * Gets the network info from a friendly name.
	 *
	 * @param friendlyName The friendly name.
	 * @return The network info.
	 */
	public static NetworkInfo fromFriendlyName(final String friendlyName) {
		switch (friendlyName) {
			case "mainnet":
				return NetworkInfos.getMainNetworkInfo();
			case "testnet":
				return NetworkInfos.getTestNetworkInfo();
			case "mijinnet":
				return NetworkInfos.getMijinNetworkInfo();
		}

		throw new IllegalArgumentException(String.format("unknown network name %s", friendlyName));
	}

	/**
	 * Gets information about the DEFAULT network.
	 *
	 * @return Information about the DEFAULT network.
	 */
	public static NetworkInfo getDefault() {
		return NETWORK_INFO.get();
	}

	/**
	 * Sets the default network.
	 *
	 * @param networkInfo The default network info.
	 */
	public static void setDefault(final NetworkInfo networkInfo) {
		NETWORK_INFO.set(networkInfo);
		MosaicConstants.setAccounts();
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

	private static NetworkInfo createMijinNetworkInfo() {
		final byte version = (byte)0x60;
		final Address nemesisAddress = Address.fromPublicKey(
				version,
				PublicKey.fromHexString("57b4832d9232ee410e93d595207cffc2b9e9c5002472c4b0bb3bb10a4ce152e3"));
		return new NetworkInfo(
				(byte)0x60,
				'M',
				new NemesisBlockInfo(
						Hash.fromHexString("16ed3d69d3ca67132aace4405aa122e5e041e58741a4364255b15201f5aaf6e4"),
						nemesisAddress,
						Amount.fromNem(9000000000L),
						"nemesis-mijinnet.bin"));
	}
}

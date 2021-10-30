package org.nem.nis;

import org.nem.core.model.NetworkInfos;

/**
 * Hard fork constants.
 */
public class BlockMarkerConstants {

	/**
	 * Hard fork due to:<br>
	 * - Switch from multisig aggregate modification transaction V1 to V2<br>
	 * - Longer messages in transfer transaction
	 */
	public static long MULTISIG_M_OF_N_FORK(final int version) {
		final byte network = (byte) (version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 199800 // 156600 + 1440 * 30
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 90000);
	}

	/**
	 * Hard fork due to<br>
	 * - namespaces<br>
	 * - mosaics (creation, supply, transfer)
	 */
	public static long MOSAICS_FORK(final int version) {
		final byte network = (byte) (version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 440_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 180000);
	}

	/**
	 * Hard fork due to<br>
	 * - changing fee structure<br>
	 */
	public static long FEE_FORK(final int version) {
		final byte network = (byte) (version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 875_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 572_500);
	}

	/**
	 * Hard fork due to<br>
	 * - changing remote account validation<br>
	 * - changing max message size
	 */
	public static long REMOTE_ACCOUNT_FORK(final int version) {
		final byte network = (byte) (version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 1_025_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 830_000);
	}

	/**
	 * Hard fork due to<br>
	 * - changing mosaic definition change handling
	 */
	public static long MOSAIC_REDEFINITION_FORK(final int version) {
		final byte network = (byte) (version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 1_110_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 871_500);
	}

	/**
	 * Second hard fork due to<br>
	 * - changing fee structure
	 */
	public static long SECOND_FEE_FORK(final int version) {
		final byte network = (byte) (version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 1_250_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 975_000);
	}
}

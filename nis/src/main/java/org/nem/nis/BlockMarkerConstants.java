package org.nem.nis;

import org.nem.core.model.NetworkInfos;

/**
 * Hard fork constants.
 */
public class BlockMarkerConstants {

	/**
	 * Hard fork due to:
	 * - Switch from multisig aggregate modification transaction V1 to V2
	 * - Longer messages in transfer transaction
	 */
	public static long MULTISIG_M_OF_N_FORK(final int version) {
		final byte network = (byte)(version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 199800 // 156600 + 1440 * 30
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 90000);
	}

	/**
	 * Hard fork due to
	 * - namespaces
	 * - mosaics (creation, supply, transfer)
	 */
	public static long MOSAICS_FORK(final int version) {
		final byte network = (byte)(version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 440_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 180000);
	}

	/**
	 * Hard fork due to
	 * - changing fee structure
	 */
	public static long FEE_FORK(final int version) {
		final byte network = (byte)(version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 875_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 572_500);
	}

	/**
	 * Hard fork due to
	 * - changing remote account validation
	 * - changing max message size
	 */
	public static long REMOTE_ACCOUNT_FORK(final int version) {
		final byte network = (byte)(version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 1_025_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 830_000);
	}

	/**
	 * Hard fork due to
	 * - changing mosaic definition change handling
	 */
	public static long MOSAIC_REDEFINITION_FORK(final int version) {
		final byte network = (byte)(version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 1_090_000
				: (network == NetworkInfos.getMijinNetworkInfo().getVersion() ? 1 : 871_500);
	}
}

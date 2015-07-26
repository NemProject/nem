package org.nem.nis;

import org.nem.core.model.NetworkInfos;

/**
 * Hard fork constants.
 */
public class BlockMarkerConstants {

	/**
	 * Hard fork due to:
	 * - Switch from multisig aggregate modification transaction V1 to V2
	 */
	public static long MULTISIG_M_OF_N_FORK(final int version) {
		final byte network = (byte)(version >> 24);
		return network == NetworkInfos.getMainNetworkInfo().getVersion()
				? 156600 + 1440 * 30
				: 90000;
	}
}

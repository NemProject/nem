package org.nem.nis;

import org.nem.core.model.NetworkInfos;

/**
 * Hard fork constants.
 */
public class BlockMarkerConstants {

	/**
	 * Hard fork due to:
	 * Switch from multisig aggregate modification transaction V1 to V2
	 */
	public static final long MULTISIG_M_OF_N_FORK(int version) {
		byte network = (byte)(version >> 24);
		if (network == NetworkInfos.getMainNetworkInfo().getVersion()) {
			return 156600 + 1440*30;
		} else {
			return 90000;
		}
	}
}

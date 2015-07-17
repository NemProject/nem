package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.NetworkInfos;

public class BlockMarkerConstantsTest {
	public static final int TESTNET_VERSION = NetworkInfos.getTestNetworkInfo().getVersion();
	public static final int MAINNET_VERSION = NetworkInfos.getMainNetworkInfo().getVersion();

	@Test
	public void testnetVersionReturns90k() {
		long marker = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(TESTNET_VERSION << 24);
		Assert.assertThat(marker, IsEqual.equalTo(90000L));
	}

	@Test
	public void mainnetVersionReturns199800() {
		long marker = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(MAINNET_VERSION << 24);
		Assert.assertThat(marker, IsEqual.equalTo(199800L));
	}

	@Test
	public void unknownVersionReturns90k() {
		long marker = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(0);
		Assert.assertThat(marker, IsEqual.equalTo(90000L));
	}
}
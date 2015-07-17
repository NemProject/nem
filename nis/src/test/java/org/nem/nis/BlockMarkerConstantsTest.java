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
		// Assert:
		assertMultisigMOfNFork(TESTNET_VERSION, 90000L);
	}

	@Test
	public void mainnetVersionReturns199800() {
		// Assert:
		assertMultisigMOfNFork(MAINNET_VERSION, 199800L);
	}

	@Test
	public void unknownNetworkVersionReturns90k() {
		// Assert:
		assertMultisigMOfNFork(0, 90000L);
	}

	private static void assertMultisigMOfNFork(final int version, final long expectedForkHeight) {
		// Act:
		long marker = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(version << 24);

		// Assert:
		Assert.assertThat(marker, IsEqual.equalTo(expectedForkHeight));
	}
}
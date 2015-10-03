package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.NetworkInfos;

public class BlockMarkerConstantsTest {
	private static final int TESTNET_VERSION = NetworkInfos.getTestNetworkInfo().getVersion();
	private static final int MAINNET_VERSION = NetworkInfos.getMainNetworkInfo().getVersion();

	//region MULTISIG_M_OF_N_FORK

	@Test
	public void multisigMOfNForkTestnetVersionReturns90k() {
		// Assert:
		assertMultisigMOfNFork(TESTNET_VERSION, 90000L);
	}

	@Test
	public void multisigMOfNForkMainnetVersionReturns199800() {
		// Assert:
		assertMultisigMOfNFork(MAINNET_VERSION, 199800L);
	}

	@Test
	public void multisigMOfNForkUnknownNetworkVersionReturns90k() {
		// Assert:
		assertMultisigMOfNFork(0, 90000L);
	}

	private static void assertMultisigMOfNFork(final int version, final long expectedForkHeight) {
		// Act:
		final long marker = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(version << 24);

		// Assert:
		Assert.assertThat(marker, IsEqual.equalTo(expectedForkHeight));
	}

	//endregion MOSAICS_FORK

	@Test
	public void mosaicsForkTestnetVersionReturns180k() {
		// Assert:
		assertMosaicsFork(TESTNET_VERSION, 180000L);
	}

	@Test
	public void mosaicsForkMainnetVersionReturns243000() {
		// Assert:
		assertMosaicsFork(MAINNET_VERSION, 243000L);
	}

	@Test
	public void mosaicsForkUnknownNetworkVersionReturns180k() {
		// Assert:
		assertMosaicsFork(0, 180000L);
	}

	private static void assertMosaicsFork(final int version, final long expectedForkHeight) {
		// Act:
		final long marker = BlockMarkerConstants.MOSAICS_FORK(version << 24);

		// Assert:
		Assert.assertThat(marker, IsEqual.equalTo(expectedForkHeight));
	}

	//endregion
}
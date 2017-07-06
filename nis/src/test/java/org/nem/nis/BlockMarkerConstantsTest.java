package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.NetworkInfos;

public class BlockMarkerConstantsTest {
	private static final int TESTNET_VERSION = NetworkInfos.getTestNetworkInfo().getVersion();
	private static final int MAINNET_VERSION = NetworkInfos.getMainNetworkInfo().getVersion();
	private static final int MIJINNET_VERSION = NetworkInfos.getMijinNetworkInfo().getVersion();

	//region MULTISIG_M_OF_N_FORK

	@Test
	public void multisigMOfNForkTestnetVersionReturns90k() {
		// Assert:
		assertMultisigMOfNFork(TESTNET_VERSION, 90_000L);
	}

	@Test
	public void multisigMOfNForkMainnetVersionReturns199800() {
		// Assert:
		assertMultisigMOfNFork(MAINNET_VERSION, 199_800L);
	}

	@Test
	public void multisigMOfNForkMijinnetVersionReturns1() {
		// Assert:
		assertMultisigMOfNFork(MIJINNET_VERSION, 1L);
	}

	@Test
	public void multisigMOfNForkUnknownNetworkVersionReturns90k() {
		// Assert:
		assertMultisigMOfNFork(0, 90_000L);
	}

	private static void assertMultisigMOfNFork(final int version, final long expectedForkHeight) {
		// Act:
		final long marker = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(version << 24);

		// Assert:
		Assert.assertThat(marker, IsEqual.equalTo(expectedForkHeight));
	}

	//region MOSAICS_FORK

	@Test
	public void mosaicsForkTestnetVersionReturns180k() {
		// Assert:
		assertMosaicsFork(TESTNET_VERSION, 180_000L);
	}

	@Test
	public void mosaicsForkMainnetVersionReturns440000() {
		// Assert:
		assertMosaicsFork(MAINNET_VERSION, 440_000L);
	}

	@Test
	public void mosaicsForkMijinnetVersionReturns1() {
		// Assert:
		assertMosaicsFork(MIJINNET_VERSION, 1L);
	}

	@Test
	public void mosaicsForkUnknownNetworkVersionReturns180k() {
		// Assert:
		assertMosaicsFork(0, 180_000L);
	}

	private static void assertMosaicsFork(final int version, final long expectedForkHeight) {
		// Act:
		final long marker = BlockMarkerConstants.MOSAICS_FORK(version << 24);

		// Assert:
		Assert.assertThat(marker, IsEqual.equalTo(expectedForkHeight));
	}

	//endregion

	//region FEE_FORK

	@Test
	public void feeForkTestnetVersionReturns572500() {
		// Assert:
		assertFeeFork(TESTNET_VERSION, 572_500L);
	}

	@Test
	public void feeForkMainnetVersionReturns875000() {
		// Assert:
		assertFeeFork(MAINNET_VERSION, 875_000L);
	}

	@Test
	public void feeForkMijinnetVersionReturns1() {
		// Assert:
		assertFeeFork(MIJINNET_VERSION, 1L);
	}

	@Test
	public void feeForkUnknownNetworkVersionReturns572500() {
		// Assert:
		assertFeeFork(0, 572_500L);
	}

	private static void assertFeeFork(final int version, final long expectedForkHeight) {
		// Act:
		final long marker = BlockMarkerConstants.FEE_FORK(version << 24);

		// Assert:
		Assert.assertThat(marker, IsEqual.equalTo(expectedForkHeight));
	}

	//endregion

	//region SECOND_FEE_FORK

	@Test
	public void secondFeeForkTestnetVersionReturns975000() {
		// Assert:
		assertSecondFeeFork(TESTNET_VERSION, 975_000);
	}

	@Test
	public void secondFeeForkMainnetVersionReturns1190000() {
		// Assert:
		assertSecondFeeFork(MAINNET_VERSION, 1_190_000);
	}

	@Test
	public void secondFeeForkMijinnetVersionReturns1() {
		// Assert:
		assertSecondFeeFork(MIJINNET_VERSION, 1L);
	}

	@Test
	public void secondFeeForkUnknownNetworkVersionReturns975000() {
		// Assert:
		assertSecondFeeFork(0, 975_000);
	}

	private static void assertSecondFeeFork(final int version, final long expectedForkHeight) {
		// Act:
		final long marker = BlockMarkerConstants.SECOND_FEE_FORK(version << 24);

		// Assert:
		Assert.assertThat(marker, IsEqual.equalTo(expectedForkHeight));
	}

	//endregion
}

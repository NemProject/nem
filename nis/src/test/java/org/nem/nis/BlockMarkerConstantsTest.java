package org.nem.nis;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.NetworkInfos;

public class BlockMarkerConstantsTest {
	private static final int TESTNET_VERSION = NetworkInfos.getTestNetworkInfo().getVersion();
	private static final int MAINNET_VERSION = NetworkInfos.getMainNetworkInfo().getVersion();
	private static final int MIJINNET_VERSION = NetworkInfos.getMijinNetworkInfo().getVersion();

	@Test
	public void multisigMOfNForkReturnsCorrectHeights() {
		// Arrange:
		final HashMap<Integer, Long> expectedMappings = new HashMap<Integer, Long>();
		expectedMappings.put(MAINNET_VERSION, 199_800L);
		expectedMappings.put(TESTNET_VERSION, 90_000L);
		expectedMappings.put(MIJINNET_VERSION, 1L);
		expectedMappings.put(0, 1L);

		expectedMappings.forEach((version, expectedMarker) -> {
			// Act:
			final long marker = BlockMarkerConstants.MULTISIG_M_OF_N_FORK(version << 24);

			// Assert:
			MatcherAssert.assertThat(String.format("%s network", version), marker, IsEqual.equalTo(expectedMarker));
		});
	}

	@Test
	public void mosaicsForkReturnsCorrectHeights() {
		// Arrange:
		final HashMap<Integer, Long> expectedMappings = new HashMap<Integer, Long>();
		expectedMappings.put(MAINNET_VERSION, 440_000L);
		expectedMappings.put(TESTNET_VERSION, 180_000L);
		expectedMappings.put(MIJINNET_VERSION, 1L);
		expectedMappings.put(0, 1L);

		expectedMappings.forEach((version, expectedMarker) -> {
			// Act:
			final long marker = BlockMarkerConstants.MOSAICS_FORK(version << 24);

			// Assert:
			MatcherAssert.assertThat(String.format("%s network", version), marker, IsEqual.equalTo(expectedMarker));
		});
	}

	@Test
	public void feeForkReturnsCorrectHeights() {
		// Arrange:
		final HashMap<Integer, Long> expectedMappings = new HashMap<Integer, Long>();
		expectedMappings.put(MAINNET_VERSION, 875_000L);
		expectedMappings.put(TESTNET_VERSION, 572_500L);
		expectedMappings.put(MIJINNET_VERSION, 1L);
		expectedMappings.put(0, 1L);

		expectedMappings.forEach((version, expectedMarker) -> {
			// Act:
			final long marker = BlockMarkerConstants.FEE_FORK(version << 24);

			// Assert:
			MatcherAssert.assertThat(String.format("%s network", version), marker, IsEqual.equalTo(expectedMarker));
		});
	}

	@Test
	public void remoteAccountForkReturnsCorrectHeights() {
		// Arrange:
		final HashMap<Integer, Long> expectedMappings = new HashMap<Integer, Long>();
		expectedMappings.put(MAINNET_VERSION, 1_025_000L);
		expectedMappings.put(TESTNET_VERSION, 830_000L);
		expectedMappings.put(MIJINNET_VERSION, 1L);
		expectedMappings.put(0, 1L);

		expectedMappings.forEach((version, expectedMarker) -> {
			// Act:
			final long marker = BlockMarkerConstants.REMOTE_ACCOUNT_FORK(version << 24);

			// Assert:
			MatcherAssert.assertThat(String.format("%s network", version), marker, IsEqual.equalTo(expectedMarker));
		});
	}

	@Test
	public void mosaicRedefinitionForkReturnsCorrectHeights() {
		// Arrange:
		final HashMap<Integer, Long> expectedMappings = new HashMap<Integer, Long>();
		expectedMappings.put(MAINNET_VERSION, 1_110_000L);
		expectedMappings.put(TESTNET_VERSION, 871_500L);
		expectedMappings.put(MIJINNET_VERSION, 1L);
		expectedMappings.put(0, 1L);

		expectedMappings.forEach((version, expectedMarker) -> {
			// Act:
			final long marker = BlockMarkerConstants.MOSAIC_REDEFINITION_FORK(version << 24);

			// Assert:
			MatcherAssert.assertThat(String.format("%s network", version), marker, IsEqual.equalTo(expectedMarker));
		});
	}

	@Test
	public void secondFeeForkReturnsCorrectHeights() {
		// Arrange:
		final HashMap<Integer, Long> expectedMappings = new HashMap<Integer, Long>();
		expectedMappings.put(MAINNET_VERSION, 1_250_000L);
		expectedMappings.put(TESTNET_VERSION, 975_000L);
		expectedMappings.put(MIJINNET_VERSION, 1L);
		expectedMappings.put(0, 1L);

		expectedMappings.forEach((version, expectedMarker) -> {
			// Act:
			final long marker = BlockMarkerConstants.SECOND_FEE_FORK(version << 24);

			// Assert:
			MatcherAssert.assertThat(String.format("%s network", version), marker, IsEqual.equalTo(expectedMarker));
		});
	}
}

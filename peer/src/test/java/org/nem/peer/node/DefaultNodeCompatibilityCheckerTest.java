package org.nem.peer.node;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.node.*;

public class DefaultNodeCompatibilityCheckerTest {
	private static final NodeMetaData ZERO_VERSION = createMetaDataForNetwork(NodeVersion.ZERO, 4);
	private static final NodeMetaData ZERO_VERSION_WITH_TAG = createMetaDataForNetwork(new NodeVersion(0, 0, 0, "tag"), 4);
	private static final NodeMetaData DEFAULT_VERSION = createMetaDataForNetwork(new NodeVersion(7, 12, 10), 4);

	// region network ids

	@Test
	public void zeroLocalVersionFailsCheckWhenNetworkIdsDoNotMatch() {
		// Arrange:
		final NodeCompatibilityChecker checker = new DefaultNodeCompatibilityChecker();
		final NodeMetaData crossNetworkMetaData = createMetaDataForNetwork(DEFAULT_VERSION.getVersion(), 7);

		// Act:
		final boolean result1 = checker.check(ZERO_VERSION_WITH_TAG, crossNetworkMetaData);
		final boolean result2 = checker.check(ZERO_VERSION, crossNetworkMetaData);

		// Assert:
		MatcherAssert.assertThat(result1, IsEqual.equalTo(false));
		MatcherAssert.assertThat(result2, IsEqual.equalTo(false));
	}

	@Test
	public void zeroRemoteVersionFailsCheckWhenNetworkIdsDoNotMatch() {
		// Arrange:
		final NodeCompatibilityChecker checker = new DefaultNodeCompatibilityChecker();
		final NodeMetaData crossNetworkMetaData = createMetaDataForNetwork(DEFAULT_VERSION.getVersion(), 7);

		// Act:
		final boolean result1 = checker.check(crossNetworkMetaData, ZERO_VERSION_WITH_TAG);
		final boolean result2 = checker.check(crossNetworkMetaData, ZERO_VERSION);

		// Assert:
		MatcherAssert.assertThat(result1, IsEqual.equalTo(false));
		MatcherAssert.assertThat(result2, IsEqual.equalTo(false));
	}

	@Test
	public void matchingVersionsFailCheckWhenNetworkIdsDoNotMatch() {
		// Arrange:
		final NodeCompatibilityChecker checker = new DefaultNodeCompatibilityChecker();
		final NodeMetaData crossNetworkMetaData = createMetaDataForNetwork(DEFAULT_VERSION.getVersion(), 7);

		// Act:
		final boolean result1 = checker.check(crossNetworkMetaData, DEFAULT_VERSION);
		final boolean result2 = checker.check(DEFAULT_VERSION, crossNetworkMetaData);

		// Assert:
		MatcherAssert.assertThat(result1, IsEqual.equalTo(false));
		MatcherAssert.assertThat(result2, IsEqual.equalTo(false));
	}

	// endregion

	// region version

	@Test
	public void zeroLocalVersionAlwaysPassesCheckWhenNetworkIdsMatch() {
		// Arrange:
		final NodeCompatibilityChecker checker = new DefaultNodeCompatibilityChecker();

		// Act:
		final boolean result1 = checker.check(ZERO_VERSION_WITH_TAG, DEFAULT_VERSION);
		final boolean result2 = checker.check(ZERO_VERSION, DEFAULT_VERSION);

		// Assert:
		MatcherAssert.assertThat(result1, IsEqual.equalTo(true));
		MatcherAssert.assertThat(result2, IsEqual.equalTo(true));
	}

	@Test
	public void zeroRemoteVersionAlwaysPassesCheckWhenNetworkIdsMatch() {
		// Arrange:
		final NodeCompatibilityChecker checker = new DefaultNodeCompatibilityChecker();

		// Act:
		final boolean result1 = checker.check(DEFAULT_VERSION, ZERO_VERSION_WITH_TAG);
		final boolean result2 = checker.check(DEFAULT_VERSION, ZERO_VERSION);

		// Assert:
		MatcherAssert.assertThat(result1, IsEqual.equalTo(true));
		MatcherAssert.assertThat(result2, IsEqual.equalTo(true));
	}

	@Test
	public void mismatchedMajorVersionsFailCheck() {
		assertVersionCompatibility(new NodeVersion(7, 12, 10), new NodeVersion(8, 12, 10), false);
	}

	@Test
	public void mismatchedMinorVersionsFailCheck() {
		assertVersionCompatibility(new NodeVersion(7, 12, 10), new NodeVersion(7, 11, 10), false);
	}

	@Test
	public void mismatchedBuildVersionsPassCheck() {
		assertVersionCompatibility(new NodeVersion(7, 11, 10), new NodeVersion(7, 11, 9), true);
	}

	@Test
	public void mismatchedTagVersionsPassCheck() {
		assertVersionCompatibility(new NodeVersion(7, 11, 9, "foo"), new NodeVersion(7, 11, 9, "bar"), true);
	}

	@Test
	public void matchingVersionsPassCheck() {
		assertVersionCompatibility(new NodeVersion(7, 11, 10), new NodeVersion(7, 11, 10), true);
		assertVersionCompatibility(new NodeVersion(7, 11, 10, "tag"), new NodeVersion(7, 11, 10, "tag"), true);
		assertVersionCompatibility(new NodeVersion(7, 11, 10), new NodeVersion(7, 11, 10, "tag"), true);
	}

	@Test
	public void minorVersionsSixAndSevenAreAllowedToCommunicateWhenMajorVersionIsZero() {
		assertVersionCompatibility(new NodeVersion(0, 6, 10), new NodeVersion(0, 7, 9), true);
		assertVersionCompatibility(new NodeVersion(0, 7, 10), new NodeVersion(0, 6, 9), true);
	}

	@Test
	public void minorVersionsSixAndSevenAreNotAllowedToCommunicateWhenMajorVersionIsNonzero() {
		assertVersionCompatibility(new NodeVersion(7, 6, 10), new NodeVersion(7, 7, 9), false);
		assertVersionCompatibility(new NodeVersion(7, 7, 10), new NodeVersion(7, 6, 9), false);
	}

	// endregion

	private static void assertVersionCompatibility(final NodeVersion version1, final NodeVersion version2,
			final boolean expectedIsCompatible) {
		// Arrange:
		final NodeCompatibilityChecker checker = new DefaultNodeCompatibilityChecker();

		// Act:
		final boolean result1 = checker.check(createMetaDataForNetwork(version1, 4), createMetaDataForNetwork(version2, 4));
		final boolean result2 = checker.check(createMetaDataForNetwork(version2, 4), createMetaDataForNetwork(version1, 4));

		// Assert:
		MatcherAssert.assertThat(result1, IsEqual.equalTo(expectedIsCompatible));
		MatcherAssert.assertThat(result2, IsEqual.equalTo(expectedIsCompatible));
	}

	private static NodeMetaData createMetaDataForNetwork(final NodeVersion nodeVersion, final int networkId) {
		return new NodeMetaData("p", "a", nodeVersion, networkId, 7);
	}
}

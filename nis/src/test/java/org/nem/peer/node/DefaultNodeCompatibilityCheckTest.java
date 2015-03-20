package org.nem.peer.node;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.node.*;

public class DefaultNodeCompatibilityCheckTest {

	@Test
	public void zeroLocalVersionAlwaysPassesCheck() {
		// Arrange:
		final NodeCompatibilityCheck checker = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = checker.check(createMetaData(0, 0, 0, "foo"), createMetaData(7, 12, 10));
		final boolean result2 = checker.check(createMetaData(0, 0, 0), createMetaData(7, 12, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
	}

	@Test
	public void zeroRemoteVersionAlwaysPassesCheck() {
		// Arrange:
		final NodeCompatibilityCheck checker = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = checker.check(createMetaData(7, 12, 10, "foo"), createMetaData(0, 0, 0));
		final boolean result2 = checker.check(createMetaData(7, 12, 10), createMetaData(0, 0, 0));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
	}

	@Test
	public void mismatchedMajorVersionsFailCheck() {
		// Arrange:
		final NodeCompatibilityCheck checker = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = checker.check(createMetaData(7, 12, 10), createMetaData(8, 12, 10));
		final boolean result2 = checker.check(createMetaData(8, 12, 10), createMetaData(7, 12, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(false));
		Assert.assertThat(result2, IsEqual.equalTo(false));
	}

	@Test
	public void mismatchedMinorVersionsFailCheck() {
		// Arrange:
		final NodeCompatibilityCheck checker = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = checker.check(createMetaData(7, 12, 10), createMetaData(7, 11, 10));
		final boolean result2 = checker.check(createMetaData(7, 11, 10), createMetaData(7, 12, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(false));
		Assert.assertThat(result2, IsEqual.equalTo(false));
	}

	@Test
	public void mismatchedBuildVersionsPassCheck() {
		// Arrange:
		final NodeCompatibilityCheck checker = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = checker.check(createMetaData(7, 11, 10), createMetaData(7, 11, 9));
		final boolean result2 = checker.check(createMetaData(7, 11, 9), createMetaData(7, 11, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
	}

	private static NodeMetaData createMetaData(final int majorVersion, final int minorVersion, final int buildVersion) {
		return new NodeMetaData("p", "a", new NodeVersion(majorVersion, minorVersion, buildVersion), 4, 7);
	}

	private static NodeMetaData createMetaData(final int majorVersion, final int minorVersion, final int buildVersion, final String tag) {
		return new NodeMetaData("p", "a", new NodeVersion(majorVersion, minorVersion, buildVersion, tag), 4, 7);
	}
}
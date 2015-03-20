package org.nem.peer.node;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.node.NodeVersion;

public class DefaultNodeCompatibilityCheckTest {

	@Test
	public void zeroLocalVersionAlwaysPassesCheck() {
		// Arrange:
		final NodeCompatibilityCheck versionCheck = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = versionCheck.check(new NodeVersion(0, 0, 0, "foo"), new NodeVersion(7, 12, 10));
		final boolean result2 = versionCheck.check(new NodeVersion(0, 0, 0), new NodeVersion(7, 12, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
	}

	@Test
	public void zeroRemoteVersionAlwaysPassesCheck() {
		// Arrange:
		final NodeCompatibilityCheck versionCheck = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = versionCheck.check(new NodeVersion(7, 12, 10, "foo"), new NodeVersion(0, 0, 0));
		final boolean result2 = versionCheck.check(new NodeVersion(7, 12, 10), new NodeVersion(0, 0, 0));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
	}

	@Test
	public void mismatchedMajorVersionsFailCheck() {
		// Arrange:
		final NodeCompatibilityCheck versionCheck = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = versionCheck.check(new NodeVersion(7, 12, 10), new NodeVersion(8, 12, 10));
		final boolean result2 = versionCheck.check(new NodeVersion(8, 12, 10), new NodeVersion(7, 12, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(false));
		Assert.assertThat(result2, IsEqual.equalTo(false));
	}

	@Test
	public void mismatchedMinorVersionsFailCheck() {
		// Arrange:
		final NodeCompatibilityCheck versionCheck = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = versionCheck.check(new NodeVersion(7, 12, 10), new NodeVersion(7, 11, 10));
		final boolean result2 = versionCheck.check(new NodeVersion(7, 11, 10), new NodeVersion(7, 12, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(false));
		Assert.assertThat(result2, IsEqual.equalTo(false));
	}

	@Test
	public void mismatchedBuildVersionsPassCheck() {
		// Arrange:
		final NodeCompatibilityCheck versionCheck = new DefaultNodeCompatibilityCheck();

		// Act:
		final boolean result1 = versionCheck.check(new NodeVersion(7, 11, 10), new NodeVersion(7, 11, 9));
		final boolean result2 = versionCheck.check(new NodeVersion(7, 11, 9), new NodeVersion(7, 11, 10));

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(true));
		Assert.assertThat(result2, IsEqual.equalTo(true));
	}
}
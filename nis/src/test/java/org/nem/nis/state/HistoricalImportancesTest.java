package org.nem.nis.state;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.ExceptionAssert;

public class HistoricalImportancesTest {

	@Test
	public void newHistoricalImportancesHasZeroSize() {
		// Arrange:
		final HistoricalImportances importances = new HistoricalImportances();

		// Assert:
		Assert.assertThat(importances.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canAddHistoricalImportance() {
		// Arrange:
		final HistoricalImportances importances = new HistoricalImportances();

		// Act:
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(13), 0.3, 0.5));
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(18), 0.1, 0.6));

		// Assert:
		assertDefaultHistoricalImportances(importances);
	}

	/*
	@Test
	public void cannotAddHistoricalImportanceAtSameHeightTwice() {
		// Arrange:
		final HistoricalImportances importances = new HistoricalImportances();

		// Act:
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(13), 0.3, 0.5));

		// Assert:
		ExceptionAssert.assertThrows(
				v -> importances.addHistoricalImportance(new AccountImportance(new BlockHeight(13), 0.3, 0.5)),
				IllegalArgumentException.class);
	}
	*/

	@Test
	public void canReplaceHistoricalImportanceIfAlreadySet() {
		// Arrange:
		final BlockHeight height = new BlockHeight(13);
		final HistoricalImportances importances = createDefaultHistoricalImportances();
		importances.addHistoricalImportance(new AccountImportance(height, 0.2, 0.4));

		// Act:
		importances.addHistoricalImportance(new AccountImportance(height, 0.3, 0.5));

		// Assert:
		Assert.assertThat(importances.getHistoricalImportance(height), IsEqual.equalTo(0.3));
		Assert.assertThat(importances.getHistoricalPageRank(height), IsEqual.equalTo(0.5));
	}

	@Test
	public void getHistoricalImportanceReturnsExpectedValueIfImportanceIsSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		Assert.assertThat(importances.getHistoricalImportance(new BlockHeight(13)), IsEqual.equalTo(0.3));
		Assert.assertThat(importances.getHistoricalImportance(new BlockHeight(18)), IsEqual.equalTo(0.1));
	}

	@Test
	public void getHistoricalImportanceReturnsZeroIfImportanceIsNotSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		Assert.assertThat(importances.getHistoricalImportance(new BlockHeight(10)), IsEqual.equalTo(0.0));
		Assert.assertThat(importances.getHistoricalImportance(new BlockHeight(21)), IsEqual.equalTo(0.0));
	}

	@Test
	public void getHistoricalPageRankReturnsExpectedValueIfPageRankIsSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		Assert.assertThat(importances.getHistoricalPageRank(new BlockHeight(13)), IsEqual.equalTo(0.5));
		Assert.assertThat(importances.getHistoricalPageRank(new BlockHeight(18)), IsEqual.equalTo(0.6));
	}

	@Test
	public void getHistoricalPageRankReturnsZeroIfPageRankIsNotSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		Assert.assertThat(importances.getHistoricalPageRank(new BlockHeight(10)), IsEqual.equalTo(0.0));
		Assert.assertThat(importances.getHistoricalPageRank(new BlockHeight(21)), IsEqual.equalTo(0.0));
	}

	@Test
	public void canCopyHistoricalImportances() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Act:
		final HistoricalImportances copy = importances.copy();

		// Assert:
		assertDefaultHistoricalImportances(copy);
	}

	@Test
	public void pruneClearsHistoricalImportances() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Act:
		importances.prune();

		// Assert:
		Assert.assertThat(importances.size(), IsEqual.equalTo(0));
	}

	private static HistoricalImportances createDefaultHistoricalImportances() {
		final HistoricalImportances importances = new HistoricalImportances();
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(13), 0.3, 0.5));
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(18), 0.1, 0.6));
		return importances;
	}

	private static void assertDefaultHistoricalImportances(final HistoricalImportances importances) {
		// Assert:
		Assert.assertThat(importances.size(), IsEqual.equalTo(2));
		Assert.assertThat(importances.getHistoricalImportance(new BlockHeight(13)), IsEqual.equalTo(0.3));
		Assert.assertThat(importances.getHistoricalImportance(new BlockHeight(18)), IsEqual.equalTo(0.1));
		Assert.assertThat(importances.getHistoricalPageRank(new BlockHeight(13)), IsEqual.equalTo(0.5));
		Assert.assertThat(importances.getHistoricalPageRank(new BlockHeight(18)), IsEqual.equalTo(0.6));
	}
}

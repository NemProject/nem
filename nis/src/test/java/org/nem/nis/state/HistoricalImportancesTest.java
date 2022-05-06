package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

public class HistoricalImportancesTest {

	@Test
	public void newHistoricalImportancesHasZeroSize() {
		// Arrange:
		final HistoricalImportances importances = new HistoricalImportances();

		// Assert:
		MatcherAssert.assertThat(importances.size(), IsEqual.equalTo(0));
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

	@Test
	public void canReplaceHistoricalImportanceIfAlreadySet() {
		// Arrange:
		final BlockHeight height = new BlockHeight(13);
		final HistoricalImportances importances = createDefaultHistoricalImportances();
		importances.addHistoricalImportance(new AccountImportance(height, 0.2, 0.4));

		// Act:
		importances.addHistoricalImportance(new AccountImportance(height, 0.3, 0.5));

		// Assert:
		MatcherAssert.assertThat(importances.getHistoricalImportance(height), IsEqual.equalTo(0.3));
		MatcherAssert.assertThat(importances.getHistoricalPageRank(height), IsEqual.equalTo(0.5));
	}

	@Test
	public void getHistoricalImportanceReturnsExpectedValueIfImportanceIsSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		MatcherAssert.assertThat(importances.getHistoricalImportance(new BlockHeight(13)), IsEqual.equalTo(0.3));
		MatcherAssert.assertThat(importances.getHistoricalImportance(new BlockHeight(18)), IsEqual.equalTo(0.1));
	}

	@Test
	public void getHistoricalImportanceReturnsZeroIfImportanceIsNotSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		MatcherAssert.assertThat(importances.getHistoricalImportance(new BlockHeight(10)), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(importances.getHistoricalImportance(new BlockHeight(21)), IsEqual.equalTo(0.0));
	}

	@Test
	public void getHistoricalPageRankReturnsExpectedValueIfPageRankIsSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		MatcherAssert.assertThat(importances.getHistoricalPageRank(new BlockHeight(13)), IsEqual.equalTo(0.5));
		MatcherAssert.assertThat(importances.getHistoricalPageRank(new BlockHeight(18)), IsEqual.equalTo(0.6));
	}

	@Test
	public void getHistoricalPageRankReturnsZeroIfPageRankIsNotSetAtHeight() {
		// Arrange:
		final HistoricalImportances importances = createDefaultHistoricalImportances();

		// Assert:
		MatcherAssert.assertThat(importances.getHistoricalPageRank(new BlockHeight(10)), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(importances.getHistoricalPageRank(new BlockHeight(21)), IsEqual.equalTo(0.0));
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
		MatcherAssert.assertThat(importances.size(), IsEqual.equalTo(0));
	}

	private static HistoricalImportances createDefaultHistoricalImportances() {
		final HistoricalImportances importances = new HistoricalImportances();
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(13), 0.3, 0.5));
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(18), 0.1, 0.6));
		return importances;
	}

	private static void assertDefaultHistoricalImportances(final HistoricalImportances importances) {
		// Assert:
		MatcherAssert.assertThat(importances.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(importances.getHistoricalImportance(new BlockHeight(13)), IsEqual.equalTo(0.3));
		MatcherAssert.assertThat(importances.getHistoricalImportance(new BlockHeight(18)), IsEqual.equalTo(0.1));
		MatcherAssert.assertThat(importances.getHistoricalPageRank(new BlockHeight(13)), IsEqual.equalTo(0.5));
		MatcherAssert.assertThat(importances.getHistoricalPageRank(new BlockHeight(18)), IsEqual.equalTo(0.6));
	}
}

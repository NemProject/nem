package org.nem.nis.pox.poi;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;

public class PoiScorerTest {
	private static final double OUTLINK_WEIGHT = 1.25;
	private static final double IMPORTANCE_WEIGHT = 0.1337;

	// region all vectors

	@Test
	public void finalScoreIsCalculatedCorrectly() {
		// Arrange:
		final ImportanceScorer scorer = new PoiScorer();
		final ImportanceScorerContextBuilder builder = new ImportanceScorerContextBuilder();
		builder.setImportanceVector(new ColumnVector(1.00, 0.80, 0.20, 0.50, 0.60, 0.30));
		builder.setOutlinkVector(new ColumnVector(4.00, 1.00, 7.00, 9.00, 2.00, 5.00));
		builder.setVestedBalanceVector(new ColumnVector(80.0, 5.00, 140., 45.0, 40.0, 25.0));
		builder.setGraphWeightVector(new ColumnVector(1.0, 0.8, 1.0, 1.2, 1.0, 1.0));

		// Act:
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(builder.create());

		// Assert:
		// weighted-outlinks: l1norm(max(0, stakes + outlinkWeight*outlinkVector))
		final ColumnVector weightedOutlinks = new ColumnVector(4.00 * OUTLINK_WEIGHT + 80.0, 1.00 * OUTLINK_WEIGHT + 5.00,
				7.00 * OUTLINK_WEIGHT + 140., 9.00 * OUTLINK_WEIGHT + 45.0, 2.00 * OUTLINK_WEIGHT + 40.0, 5.00 * OUTLINK_WEIGHT + 25.0);
		weightedOutlinks.normalize();

		// weighted-importance: importanceWeight * PR
		final ColumnVector weightedImportance = new ColumnVector(1.00 * IMPORTANCE_WEIGHT, 0.80 * IMPORTANCE_WEIGHT,
				0.20 * IMPORTANCE_WEIGHT, 0.50 * IMPORTANCE_WEIGHT, 0.60 * IMPORTANCE_WEIGHT, 0.30 * IMPORTANCE_WEIGHT);

		// final: l1norm((weighted-outlinks + weighted-importance) * graphWeightVector)
		final ColumnVector expectedFinalScoresVector = new ColumnVector(1.0 * (weightedOutlinks.getAt(0) + weightedImportance.getAt(0)),
				0.8 * (weightedOutlinks.getAt(1) + weightedImportance.getAt(1)),
				1.0 * (weightedOutlinks.getAt(2) + weightedImportance.getAt(2)),
				1.2 * (weightedOutlinks.getAt(3) + weightedImportance.getAt(3)),
				1.0 * (weightedOutlinks.getAt(4) + weightedImportance.getAt(4)),
				1.0 * (weightedOutlinks.getAt(5) + weightedImportance.getAt(5)));
		expectedFinalScoresVector.normalize();

		MatcherAssert.assertThat(finalScoresVector, IsEqual.equalTo(expectedFinalScoresVector));
	}

	@Test
	public void finalScoreIsCalculatedCorrectlyWhenSomeWeightedOutlinksAreNegative() {
		// Arrange:
		final ImportanceScorer scorer = new PoiScorer();
		final ImportanceScorerContextBuilder builder = new ImportanceScorerContextBuilder();
		builder.setImportanceVector(new ColumnVector(1.00, 0.80, 0.20, 0.50, 0.60, 0.30));
		builder.setOutlinkVector(new ColumnVector(-4.00, 1.00, 7.00, -2.00, 9.00, 5.00));
		builder.setVestedBalanceVector(new ColumnVector(80.0, 140., -5.00, 2.00, -40.0, 25.0));
		builder.setGraphWeightVector(new ColumnVector(1.0, 0.8, 1.0, 1.2, 1.0, 1.0));

		// Act:
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(builder.create());

		// Assert:
		// weighted-outlinks: l1norm(max(0, stakes + outlinkWeight*outlinkVector))
		final ColumnVector weightedOutlinks = new ColumnVector(-4.00 * OUTLINK_WEIGHT + 80.0, 1.00 * OUTLINK_WEIGHT + 140.,
				7.00 * OUTLINK_WEIGHT + -5.00, 0, 0, 5.00 * OUTLINK_WEIGHT + 25.0);
		weightedOutlinks.normalize();

		// weighted-importance: importanceWeight * PR
		final ColumnVector weightedImportance = new ColumnVector(1.00 * IMPORTANCE_WEIGHT, 0.80 * IMPORTANCE_WEIGHT,
				0.20 * IMPORTANCE_WEIGHT, 0.50 * IMPORTANCE_WEIGHT, 0.60 * IMPORTANCE_WEIGHT, 0.30 * IMPORTANCE_WEIGHT);

		// final: l1norm((weighted-outlinks + weighted-importance) * graphWeightVector)
		final ColumnVector expectedFinalScoresVector = new ColumnVector(1.0 * (weightedOutlinks.getAt(0) + weightedImportance.getAt(0)),
				0.8 * (weightedOutlinks.getAt(1) + weightedImportance.getAt(1)),
				1.0 * (weightedOutlinks.getAt(2) + weightedImportance.getAt(2)),
				1.2 * (weightedOutlinks.getAt(3) + weightedImportance.getAt(3)),
				1.0 * (weightedOutlinks.getAt(4) + weightedImportance.getAt(4)),
				1.0 * (weightedOutlinks.getAt(5) + weightedImportance.getAt(5)));
		expectedFinalScoresVector.normalize();

		MatcherAssert.assertThat(finalScoresVector, IsEqual.equalTo(expectedFinalScoresVector));
	}

	// endregion

	// region single vectors

	@Test
	public void finalScoreIsCalculatedCorrectlyWhenOnlyInputIsImportanceVector() {
		// Arrange:
		final ImportanceScorer scorer = new PoiScorer();
		final ImportanceScorerContextBuilder builder = createBuilderWithDefaultVectors();
		builder.setImportanceVector(new ColumnVector(0.50, -0.40, 0.10));

		// Act:
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(builder.create());

		// Assert:
		final ColumnVector expectedFinalScoresVector = new ColumnVector(0.50, -0.40, 0.10);
		assertRoundedEquality(finalScoresVector, expectedFinalScoresVector);
	}

	@Test
	public void finalScoreIsCalculatedCorrectlyWhenOnlyInputIsOutlinkVector() {
		// Arrange:
		final ImportanceScorer scorer = new PoiScorer();
		final ImportanceScorerContextBuilder builder = createBuilderWithDefaultVectors();
		builder.setOutlinkVector(new ColumnVector(0.50, -0.40, 0.10));

		// Act:
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(builder.create());

		// Assert:
		final ColumnVector expectedFinalScoresVector = new ColumnVector(0.50 / 0.60, 0.00, 0.10 / 0.60);
		assertRoundedEquality(finalScoresVector, expectedFinalScoresVector);
	}

	@Test
	public void finalScoreIsCalculatedCorrectlyWhenOnlyInputIsVestedBalancesVector() {
		// Arrange:
		final ImportanceScorer scorer = new PoiScorer();
		final ImportanceScorerContextBuilder builder = createBuilderWithDefaultVectors();
		builder.setVestedBalanceVector(new ColumnVector(0.50, -0.40, 0.10));

		// Act:
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(builder.create());

		// Assert:
		final ColumnVector expectedFinalScoresVector = new ColumnVector(0.50 / 0.60, 0.00, 0.10 / 0.60);
		expectedFinalScoresVector.normalize();
		assertRoundedEquality(finalScoresVector, expectedFinalScoresVector);
	}

	@Test
	public void finalScoreIsCalculatedCorrectlyWhenOnlyInputIsGraphWeightVector() {
		// Arrange:
		final ImportanceScorer scorer = new PoiScorer();
		final ImportanceScorerContextBuilder builder = createBuilderWithDefaultVectors();
		builder.setGraphWeightVector(new ColumnVector(0.50, -0.40, 0.10));

		// Act:
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(builder.create());

		// Assert:
		final ColumnVector expectedFinalScoresVector = new ColumnVector(0.00, 0.00, 0.00);
		expectedFinalScoresVector.normalize();
		assertRoundedEquality(finalScoresVector, expectedFinalScoresVector);
	}

	// endregion

	private static ImportanceScorerContextBuilder createBuilderWithDefaultVectors() {
		final ImportanceScorerContextBuilder builder = new ImportanceScorerContextBuilder();
		builder.setImportanceVector(new ColumnVector(0.00, 0.00, 0.00));
		builder.setOutlinkVector(new ColumnVector(0.00, 0.00, 0.00));
		builder.setVestedBalanceVector(new ColumnVector(0.00, 0.00, 0.00));
		builder.setGraphWeightVector(new ColumnVector(1.0, 1.0, 1.0));
		return builder;
	}

	private static void assertRoundedEquality(final ColumnVector actual, final ColumnVector expected) {
		MatcherAssert.assertThat(actual.roundTo(10), IsEqual.equalTo(expected.roundTo(10)));
	}
}

package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;

public class PoiScorerTest {

	@Test
	public void finalScoreIsCalculatedCorrectly() {
		// Arrange
		final double outlinkWeight = 1.25;
		final double importanceWeight = 0.1337;

		// Act:
		final ImportanceScorer scorer = new PoiScorer();
		final ImportanceScorerContextBuilder builder = new ImportanceScorerContextBuilder();
		builder.setImportanceVector(new ColumnVector(1.00, 0.80, 0.20, 0.50, 0.60, 0.30));
		builder.setOutlinkVector(new ColumnVector(1.00, 0.80, 0.20, 0.50, 0.60, 0.30));
		builder.setVestedBalanceVector(new ColumnVector(4.00, 1.00, 7.00, 9.00, 2.00, 5.00));
		builder.setGraphWeightVector(new ColumnVector(1.0, 1.0, 1.0, 1.0, 1.0, 1.0));
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(builder.create());

		// Assert:
		// weighted-outlinks: l1norm(outlink * outlinkWeight + vested-balance)
		final ColumnVector weightedOutlinks = new ColumnVector(
				4.00 * outlinkWeight + 80.0,
				1.00 * outlinkWeight + 5.00,
				7.00 * outlinkWeight + 140.,
				9.00 * outlinkWeight + 45.0,
				2.00 * outlinkWeight + 40.0,
				5.00 * outlinkWeight + 25.0);
		weightedOutlinks.normalize();

		// weighted-importance: importance * 0.05
		final ColumnVector weightedImportance = new ColumnVector(
				1.00 * importanceWeight,
				0.80 * importanceWeight,
				0.20 * importanceWeight,
				0.50 * importanceWeight,
				0.60 * importanceWeight,
				0.30 * importanceWeight);

		// final: l1norm(weighted-outlinks + weighted-importance))
		final ColumnVector expectedFinalScoresVector = weightedOutlinks.addElementWise(weightedImportance);
		expectedFinalScoresVector.normalize();
		Assert.assertThat(finalScoresVector, IsEqual.equalTo(expectedFinalScoresVector));
	}
}
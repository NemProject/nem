package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;

import java.util.Arrays;

public class PoiScorerTest {

	@Test
	public void dangleSumIsCalculatedCorrectly() {
		// Act:
		final PoiScorer scorer = new PoiScorer();
		final double dangleSum = scorer.calculateDangleSum(
				Arrays.asList(1, 3),
				PoiContext.TELEPORTATION_PROB,
				new ColumnVector(0.1, 0.8, 0.2, 0.5, 0.6, 0.3));

		// Assert:
		Assert.assertThat(dangleSum, IsEqual.equalTo(0.1625));
	}

	@Test
	public void finalScoreIsCalculatedCorrectly() {
		// Arrange
		final double importanceWeight = 0.1337; //TODO: was 0.05 // TODO-CR [08062014][J-M]: why did this change?
		// TODO-CR [20140806][M-J]: with NCDawareRank, I want to see if it is safe to use a larger weight. 5% is too low IMHO
		// TODO-CR [20140921][M-J]: update: we should do more testing, but this seems safe enough IMHO

		// Act:
		final PoiScorer scorer = new PoiScorer();
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(
				new ColumnVector(1.00, 0.80, 0.20, 0.50, 0.60, 0.30),  // importance
				new ColumnVector(4.00, 1.00, 7.00, 9.00, 2.00, 5.00),  // outlink
				new ColumnVector(80.0, 5.00, 140., 45.0, 40.0, 25.0)); // vested-balance

		// Assert:
		// weighted-outlinks: outlink * 1.05 + vested-balance
		final ColumnVector weightedOutlinks = new ColumnVector(
				4.00 * 1.25 + 80.0,
				1.00 * 1.25 + 5.00,
				7.00 * 1.25 + 140.,
				9.00 * 1.25 + 45.0,
				2.00 * 1.25 + 40.0,
				5.00 * 1.25 + 25.0);

		// weighted-importance: importance * 0.05
		final ColumnVector weightedImportance = new ColumnVector(
				1.00 * importanceWeight,
				0.80 * importanceWeight,
				0.20 * importanceWeight,
				0.50 * importanceWeight,
				0.60 * importanceWeight,
				0.30 * importanceWeight);

		// final: l1norm(l1norm(weighted-outlinks) + weighted-importance)
		weightedOutlinks.normalize();
		final ColumnVector expectedFinalScoresVector = weightedOutlinks.addElementWise(weightedImportance);
		expectedFinalScoresVector.normalize();
		Assert.assertThat(finalScoresVector, IsEqual.equalTo(expectedFinalScoresVector));
	}
}
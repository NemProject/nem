package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;

import java.util.*;

public class PoiScorerTest {

	@Test
	public void dangleSumIsCalculatedCorrectly() {
		// Act:
		final PoiScorer scorer = new PoiScorer();
		double dangleSum = scorer.calculateDangleSum(
				Arrays.asList(1, 3),
				new ColumnVector(1, 2, 3, 4, 5, 6),
				new ColumnVector(0.1, 0.8, 0.2, 0.5, 0.6, 0.3));

		// Assert:
		Assert.assertThat(dangleSum, IsEqual.equalTo(0.60));
	}

	@Test
	public void finalScoreIsCalculatedCorrectly() {
		// Act:
		final PoiScorer scorer = new PoiScorer();
		final ColumnVector finalScoresVector = scorer.calculateFinalScore(
				new ColumnVector(1.00, 0.80, 0.20, 0.50, 0.60, 0.30),
				new ColumnVector(4.00, 1.00, 7.00, 9.00, 2.00, 5.00),
				new ColumnVector(80.0, 5.00, 140., 45.0, 40.0, 25.0), PoiScorer.ScoringAlg.BLOODYROOKIE);

		// Assert:
		final double scale = 1 * 9 * 140;
		final ColumnVector expectedFinalScoresVector = new ColumnVector(
				1 * 4 * 80 / scale,
				0.8 * 1 * 5 / scale,
				0.2 * 7 * 140 / scale,
				0.5 * 9 * 45 / scale,
				0.6 * 2 * 40 / scale,
				0.3 * 5 * 25 / scale);
		Assert.assertThat(finalScoresVector, IsEqual.equalTo(expectedFinalScoresVector));
	}
}
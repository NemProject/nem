package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

import java.util.*;

public class PoiScorerTest {

	@Test
	public void dangleSumIsCalculatedCorrectly() {
		// Arrange:
		final ColumnVector importanceVector = new ColumnVector(0.1, 0.8, 0.2, 0.5, 0.6, 0.3);
		final MockPoiContext context = new MockPoiContext();
		context.setTeleportationVector(new ColumnVector(1, 2, 3, 4, 5, 6));
		context.setDangleIndexes(Arrays.asList(1, 3));

		// Act:
		final PoiScorer scorer = new PoiScorer(context);

		// Assert:
		Assert.assertThat(scorer.calculateDangleSum(importanceVector), IsEqual.equalTo(0.60));
	}

	@Test
	public void finalScoreIsCalculatedCorrectly() {
		// Arrange:
		final ColumnVector importanceVector = new ColumnVector(1.00, 0.80, 0.20, 0.50, 0.60, 0.30);
		final ColumnVector outLinksVector = new ColumnVector(4.00, 1.00, 7.00, 9.00, 2.00, 5.00);
		final ColumnVector coinDaysVector = new ColumnVector(80.0, 5.00, 140., 45.0, 40.0, 25.0);
		final MockPoiContext context = new MockPoiContext();
		context.setCoinDaysVector(coinDaysVector);

		// Act:
		final PoiScorer scorer = new PoiScorer(context);

		// Assert:
		final double scale = 1 * 9 * 140;
		final ColumnVector expectedFinalScoresVector = new ColumnVector(
				1 * 4 * 80 / scale,
				0.8 * 1 * 5 / scale,
				0.2 * 7 * 140 / scale,
				0.5 * 9 * 45 / scale,
				0.6 * 2 * 40 / scale,
				0.3 * 5 * 25 / scale);
		Assert.assertThat(
				scorer.calculateFinalScore(importanceVector, outLinksVector),
				IsEqual.equalTo(expectedFinalScoresVector));
	}

	private static class MockPoiContext extends PoiContext {

		private List<Integer> dangleIndexes;
		private ColumnVector coinDaysVector;
		private ColumnVector importanceVector;
		private ColumnVector teleportationVector;
		private ColumnVector outLinkScoreVector;

		public MockPoiContext() {
			super(Arrays.asList(new Account(Utils.generateRandomAddress())), 1, new BlockHeight(21));
		}

		@Override
		public List<Integer> getDangleIndexes() {
			return this.dangleIndexes;
		}

		@Override
		public ColumnVector getCoinDaysVector() {
			return this.coinDaysVector;
		}

		@Override
		public ColumnVector getOutLinkScoreVector() {
			return this.outLinkScoreVector;
		}

		@Override
		public ColumnVector getImportanceVector() {
			return this.importanceVector;
		}

		@Override
		public ColumnVector getTeleportationVector() {
			return this.teleportationVector;
		}

		public void setDangleIndexes(final List<Integer> dangleIndexes) {
			this.dangleIndexes = dangleIndexes;
		}

		public void setCoinDaysVector(final ColumnVector coinDaysVector) {
			this.coinDaysVector = coinDaysVector;
		}

		public void getOutLinkScoreVector(final ColumnVector outLinkScoreVector) {
			this.outLinkScoreVector = teleportationVector;
		}

		public void setImportanceVector(final ColumnVector importanceVector) {
			this.importanceVector = importanceVector;
		}

		public void setTeleportationVector(final ColumnVector teleportationVector) {
			this.teleportationVector = teleportationVector;
		}
	}
}
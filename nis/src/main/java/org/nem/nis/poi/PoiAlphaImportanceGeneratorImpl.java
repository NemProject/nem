package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * This is a first draft implementation of the POI importance calculation.
 *
 * Because a lot of the infrastructure is not yet in place, I am making the
 * following assumptions in this code:
 *
 * 1) This class's calculateImportancesImpl is called with a list of all the accounts.
 * 2) POI is calculated by the forager after processing new transactions. 
 *    This algorithm is not currently iterative, so importances are calculated from scratch every time. 
 *    I plan to make this iterative so that we update importances only for accounts affected by new transactions and their links.
 *
 */
public class PoiAlphaImportanceGeneratorImpl implements PoiImportanceGenerator {

	private static final Logger LOGGER = Logger.getLogger(PoiAlphaImportanceGeneratorImpl.class.getName());

	public static final int DEFAULT_MAX_ITERS = 200;
	public static final double DEFAULT_POWER_ITERATION_TOL = 1.0e-3;

	@Override
	public ColumnVector getAccountImportances(final BlockHeight blockHeight, Collection<Account> accounts, PoiScorer.ScoringAlg scoringAlg) {
		return calculateImportancesImpl(blockHeight, accounts, scoringAlg);
	}

	// This is the draft implementation for calculating proof-of-importance
	private ColumnVector calculateImportancesImpl(
			final BlockHeight blockHeight,
			final Collection<Account> accounts,
			final PoiScorer.ScoringAlg scoringAlg) {

		// (1) set up the matrices and vectors
		final PoiContext context = new PoiContext(accounts, blockHeight);
		final PoiScorer scorer = new PoiScorer();

		// (2) run the power iteration algorithm
		final PowerIterator iterator = new PoiPowerIterator(context, scorer, accounts.size());

		long start = System.currentTimeMillis();
		iterator.run();
		long stop = System.currentTimeMillis();
		LOGGER.info("POI iterator needed " + (stop - start) + "ms.");

		if (!iterator.hasConverged()) {
			final String message = String.format("POI: power iteration failed to converge in %s iterations", DEFAULT_MAX_ITERS);
			throw new IllegalStateException(message);
		}

		// (3) merge all sub-scores
		return scorer.calculateFinalScore(
				iterator.getResult(),
				context.getOutlinkScoreVector(),
				context.getVestedBalanceVector(),
				scoringAlg);
	}

	private static class PoiPowerIterator extends PowerIterator {

		private final PoiContext context;
		private final PoiScorer scorer;

		public PoiPowerIterator(final PoiContext context, final PoiScorer scorer, final int numAccounts) {
			super(context.getImportanceVector(), DEFAULT_MAX_ITERS, DEFAULT_POWER_ITERATION_TOL / numAccounts);
			this.context = context;
			this.scorer = scorer;
		}

		@Override
		protected ColumnVector stepImpl(final ColumnVector prevIterImportances) {

			double dangleSum = this.scorer.calculateDangleSum(
					this.context.getDangleIndexes(),
					this.context.getTeleportationVector(),
					prevIterImportances);

			// V(dangle-indexes) * dangle-sum + V(1.0) - V(teleportation)
			final ColumnVector poiAdjustmentVector = this.context.getDangleVector()
					.multiply(dangleSum)
					.add(this.context.getInverseTeleportationVector());

			// M(out-link) * V(importance) .* V(teleportation)
			final ColumnVector importances = this.context.getOutlinkMatrix()
					.multiply(prevIterImportances)
					.multiplyElementWise(this.context.getTeleportationVector());

			return importances.add(poiAdjustmentVector);
		}
	}
}

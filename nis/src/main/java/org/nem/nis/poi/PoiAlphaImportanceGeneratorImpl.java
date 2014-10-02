package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.graph.*;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * This is a first draft implementation of the POI importance calculation.
 * Because a lot of the infrastructure is not yet in place, I am making the
 * following assumptions in this code:
 * 1) This class's calculateImportancesImpl is called with a list of all the accounts.
 * 2) POI is calculated by the forager after processing new transactions.
 * This algorithm is not currently iterative, so importances are calculated from scratch every time.
 * I plan to make this iterative so that we update importances only for accounts affected by new transactions and their links.
 */
public class PoiAlphaImportanceGeneratorImpl implements PoiImportanceGenerator {

	private static final Logger LOGGER = Logger.getLogger(PoiAlphaImportanceGeneratorImpl.class.getName());

	public static final int DEFAULT_MAX_ITERATIONS = 2000;
	public static final double DEFAULT_POWER_ITERATION_TOL = 1.0e-3;

	@Override
	public void updateAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates,
			final PoiScorer poiScorer,
			final GraphClusteringStrategy clusterer) {
		// This is the draft implementation for calculating proof-of-importance
		// (1) set up the matrices and vectors
		final PoiContext context = new PoiContext(accountStates, blockHeight, clusterer);
		final PoiScorer scorer = new PoiScorer();

		// (2) run the power iteration algorithm
		final PowerIterator iterator = new PoiPowerIterator(context, scorer, accountStates.size());

		final long start = System.currentTimeMillis();
		iterator.run();
		final long stop = System.currentTimeMillis();
		LOGGER.info("POI iterator needed " + (stop - start) + "ms.");

		if (!iterator.hasConverged()) {
			final String message = String.format(
					"POI: power iteration failed to converge in %s iterations",
					DEFAULT_MAX_ITERATIONS);
			throw new IllegalStateException(message);
		}

		// (3) merge all sub-scores
		final ColumnVector importanceVector = scorer.calculateFinalScore(
				iterator.getResult(),
				context.getOutlinkScoreVector(),
				context.getVestedBalanceVector());
		context.updateImportances(iterator.getResult(), importanceVector);
	}

	private static class PoiPowerIterator extends PowerIterator {

		private final PoiContext context;
		private final PoiScorer scorer;

		public PoiPowerIterator(final PoiContext context, final PoiScorer scorer, final int numAccounts) {
			super(context.getPoiStartVector(), DEFAULT_MAX_ITERATIONS, DEFAULT_POWER_ITERATION_TOL / numAccounts);
			this.context = context;
			this.scorer = scorer;
		}

		@Override
		protected ColumnVector stepImpl(final ColumnVector prevIterImportances) {
			// TODO: expose as functions
			//PoiContext.TELEPORTATION_PROB
			//PoiContext.INTER_LEVEL_TELEPORTATION_PROB

			final ColumnVector poiAdjustmentVector = this.createAdjustmentVector(prevIterImportances);
			final ColumnVector importancesVector = this.createImportancesVector(prevIterImportances);
			final ColumnVector interLevelVector = this.createInterLeverVector(prevIterImportances);

			return importancesVector
					.addElementWise(interLevelVector)
					.addElementWise(poiAdjustmentVector);
		}

		private ColumnVector createAdjustmentVector(final ColumnVector prevIterImportances) {
			final double dangleSum = this.scorer.calculateDangleSum(
					this.context.getDangleIndexes(),
					PoiContext.TELEPORTATION_PROB,
					prevIterImportances);

			// V(dangle-indexes) * dangle-sum + V(inverseTeleportation)
			return this.context.getDangleVector()
					.multiply(dangleSum)
					.add(this.context.getInverseTeleportationProb());
		}

		private ColumnVector createImportancesVector(final ColumnVector prevIterImportances) {
			// M(out-link) * V(last iter) .* V(teleportation)
			return this.context.getOutlinkMatrix()
					.multiply(prevIterImportances)
					.multiply(PoiContext.TELEPORTATION_PROB);
		}

		private ColumnVector createInterLeverVector(final ColumnVector prevIterImportances) {
			// inter-level proximity calc for NCD-Aware Rank: (R*(last iter) * A)
			final InterLevelProximityMatrix interLevelMatrix = this.context.getInterLevelMatrix();
			return interLevelMatrix.getA()
					.multiply(interLevelMatrix.getR().multiply(prevIterImportances))
					.multiply(PoiContext.INTER_LEVEL_TELEPORTATION_PROB);
		}
	}
}

package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.graph.InterLevelProximityMatrix;

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
public class PoiImportanceCalculator implements ImportanceCalculator {
	private static final Logger LOGGER = Logger.getLogger(PoiImportanceCalculator.class.getName());

	private static final int DEFAULT_MAX_ITERATIONS = 3000;
	private static final double DEFAULT_POWER_ITERATION_TOL = 1.0e-3;

	private final PoiScorer poiScorer;
	private final PoiOptions options;

	/**
	 * Creates a new generator with custom options.
	 *
	 * @param poiScorer The poi scorer to use.
	 * @param options The poi options.
	 */
	public PoiImportanceCalculator(
			final PoiScorer poiScorer,
			final PoiOptions options) {
		this.poiScorer = poiScorer;
		this.options = options;
	}

	@Override
	public void recalculate(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates) {
		// This is the draft implementation for calculating proof-of-importance
		// (1) set up the matrices and vectors
		final PoiContext context = new PoiContext(accountStates, blockHeight, this.options);

		// (2) run the power iteration algorithm
		final PowerIterator iterator = new PoiPowerIterator(
				context,
				this.options,
				this.poiScorer,
				accountStates.size(),
				this.options.isClusteringEnabled());

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
		final ColumnVector importanceVector = this.poiScorer.calculateFinalScore(
				iterator.getResult(),
				context.getOutlinkScoreVector(),
				context.getVestedBalanceVector());
		context.updateImportances(iterator.getResult(), importanceVector);
	}

	private static class PoiPowerIterator extends PowerIterator {
		private final PoiContext context;
		private final PoiOptions options;
		private final PoiScorer scorer;
		private final boolean useClustering;

		public PoiPowerIterator(
				final PoiContext context,
				final PoiOptions options,
				final PoiScorer scorer,
				final int numAccounts,
				final boolean useClustering) {
			super(context.getPoiStartVector(), DEFAULT_MAX_ITERATIONS, DEFAULT_POWER_ITERATION_TOL / numAccounts);
			this.context = context;
			this.options = options;
			this.scorer = scorer;
			this.useClustering = useClustering;
		}

		@Override
		protected ColumnVector stepImpl(final ColumnVector prevIterImportances) {
			final ColumnVector poiAdjustmentVector = this.createAdjustmentVector(prevIterImportances);
			final ColumnVector importancesVector = this.createImportancesVector(prevIterImportances);

			ColumnVector resultVector = importancesVector.addElementWise(poiAdjustmentVector);
			if (this.useClustering) {
				final ColumnVector interLevelVector = this.createInterLeverVector(prevIterImportances);
				final ColumnVector outlierVector = this.createOutlierVector();
				resultVector = resultVector
						.addElementWise(interLevelVector)
						.multiplyElementWise(outlierVector);
			}

			return resultVector;
		}

		private ColumnVector createAdjustmentVector(final ColumnVector prevIterImportances) {
			final double totalTeleportationProbability = this.options.getTeleportationProbability() + this.options.getInterLevelTeleportationProbability();
			final double dangleSum = this.scorer.calculateDangleSum(
					this.context.getDangleIndexes(),
					totalTeleportationProbability,
					prevIterImportances);

			// V(dangle-sum + inverseTeleportation / N)
			final int size = prevIterImportances.size();
			final ColumnVector dangleVector = new ColumnVector(size);
			dangleVector.setAll(dangleSum + this.options.getInverseTeleportationProbability() / size);
			return dangleVector;
		}

		private ColumnVector createImportancesVector(final ColumnVector prevIterImportances) {
			// M(out-link) * V(last iter) .* V(teleportation)
			return this.context.getOutlinkMatrix()
					.multiply(prevIterImportances)
					.multiply(this.options.getTeleportationProbability());
		}

		private ColumnVector createOutlierVector() {
			// V(1) - V(outlier) * (1 - weight) ==> V(outlier) * (-1 + weight) + V(1)
			final ColumnVector onesVector = new ColumnVector(this.context.getOutlierVector().size());
			onesVector.setAll(1);
			return this.context.getOutlierVector()
					.multiply(-1 + this.options.getOutlierWeight())
					.addElementWise(onesVector);
		}

		private ColumnVector createInterLeverVector(final ColumnVector prevIterImportances) {
			// inter-level proximity calc for NCD-Aware Rank: (R*(last iter) * A)
			final InterLevelProximityMatrix interLevelMatrix = this.context.getInterLevelMatrix();
			return interLevelMatrix.getA()
					.multiply(interLevelMatrix.getR().multiply(prevIterImportances))
					.multiply(this.options.getInterLevelTeleportationProbability());
		}
	}
}
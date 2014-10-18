package org.nem.nis.poi;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.graph.*;

import java.util.*;
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

	private final ImportanceScorer scorer;
	private final PoiOptions options;

	/**
	 * Creates a new generator with custom options.
	 *
	 * @param scorer The poi scorer to use.
	 * @param options The poi options.
	 */
	public PoiImportanceCalculator(
			final ImportanceScorer scorer,
			final PoiOptions options) {
		this.scorer = scorer;
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
		final ColumnVector importanceVector = this.scorer.calculateFinalScore(
				iterator.getResult(),
				context.getOutlinkScoreVector(),
				context.getVestedBalanceVector());
		context.updateImportances(iterator.getResult(), importanceVector);
	}

	private static class PoiPowerIterator extends PowerIterator {
		private final PoiContext context;
		private final PoiOptions options;
		// TODO 20141014 J-B: you don't need to pass scorer here
		// TODO 20141015 BR -> J: actually it was already there, I didn't add it ^^
		private final boolean useClustering;

		public PoiPowerIterator(
				final PoiContext context,
				final PoiOptions options,
				final int numAccounts,
				final boolean useClustering) {
			super(context.getPoiStartVector(), DEFAULT_MAX_ITERATIONS, DEFAULT_POWER_ITERATION_TOL / numAccounts);
			this.context = context;
			this.options = options;
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
			// TODO-CR 20141017 BR -> J,M: adding the interlevel teleportation probability leads to violation of the probability conservation.
			// TODO                        Thus the l1-norm of the importance is changed in each step. We are normalizing after each step but
			// TODO                        it still could result in some strange effects. The reason for the dangle sum is that there is some probability
			// TODO                        missing due to some columns of the outlink matrix being zero. This is exactly compensated when using the
			// TODO                        teleportation probability in the dangle sum. the dangle sum has nothing to do with the ILP matrix or
			// TODO                        the interlevel teleportation probability. I would suggest to use only the teleportation probability.
			// TODO-CR 20141018 M -> J, BR: I changed this back to just teleportation probability
			final double dangleSum = calculateDangleSum(
					this.context.getDangleIndexes(),
					this.options.getTeleportationProbability(),
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

		// TODO 20141014 J-B: i would move this somewhere so that we can keep the deleted test
		// TODO 20151015 BR -> J: tell me where to move it, it is only used in this class.
		/**
		 * Calculates the weighted teleporation sum of all dangling accounts.
		 *
		 * @param dangleIndexes The indexes of dangling accounts.
		 * @param teleportationProbability The teleportation probability.
		 * @param importanceVector The importance (weights).
		 * @return The weighted teleporation sum of all dangling accounts.
		 */
		private double calculateDangleSum(
				final List<Integer> dangleIndexes,
				final double teleportationProbability,
				final ColumnVector importanceVector) {

			double dangleSum = 0;
			for (final int i : dangleIndexes) {
				dangleSum += importanceVector.getAt(i);
			}

			return dangleSum * teleportationProbability / importanceVector.size();
		}
	}
}

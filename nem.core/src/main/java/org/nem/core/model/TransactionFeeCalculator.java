package org.nem.core.model;

import org.nem.core.model.primitive.*;

/**
 * Helper class for calculating and validating transaction fees.
 */
public class TransactionFeeCalculator {
	private static final Amount FEE_UNIT = Amount.fromNem(2);
	private static final long FEE_UNIT_NUM_NEM = FEE_UNIT.getNumNem();
	private static final int FEE_MULTIPLIER = 3;

	/**
	 * Calculates the minimum fee for the specified transaction at the specified block height.
	 *
	 * @param transaction The transaction.
	 * @param blockHeight The block height.
	 * @return The minimum fee.
	 */
	public static Amount calculateMinimumFee(final Transaction transaction, final BlockHeight blockHeight) {
		switch (transaction.getType()) {
			case TransactionTypes.TRANSFER:
				return calculateMinimumFee((TransferTransaction)transaction);

			case TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION:
				return calculateMinimumFee((MultisigAggregateModificationTransaction)transaction);
		}

		return FEE_UNIT.multiply(FEE_MULTIPLIER);
	}

	private static Amount calculateMinimumFee(final TransferTransaction transaction) {
		final long numNem = transaction.getAmount().getNumNem();
		final long messageFee = null == transaction.getMessage() ? 0 : Math.max(1, transaction.getMessageLength() / 16) * FEE_UNIT_NUM_NEM;
		final long smallTransferPenalty = FEE_UNIT.multiply(5).getNumNem() - numNem;
		final long largeTransferFee = (long)(Math.atan(numNem / 150000.) * FEE_MULTIPLIER * 33);
		final long transferFee = Math.max(smallTransferPenalty, Math.max(FEE_UNIT_NUM_NEM, largeTransferFee));
		return Amount.fromNem(messageFee + transferFee);
	}

	private static Amount calculateMinimumFee(final MultisigAggregateModificationTransaction transaction) {
		final int numModifications = transaction.getCosignatoryModifications().size();
		final int minCosignatoriesFee = null == transaction.getMinCosignatoriesModification() ? 0 : FEE_MULTIPLIER;
		return FEE_UNIT.multiply(5 + FEE_MULTIPLIER * numModifications + minCosignatoriesFee);
	}

	/**
	 * Determines whether the fee for the transaction at the specified block height is valid.
	 *
	 * @param transaction The transaction.
	 * @param blockHeight The block height.
	 * @return true if the transaction fee is valid; false otherwise.
	 */
	public static boolean isFeeValid(final Transaction transaction, final BlockHeight blockHeight) {
		final Amount minimumFee = calculateMinimumFee(transaction, blockHeight);
		switch (transaction.getType()) {
			case TransactionTypes.MULTISIG_SIGNATURE:
				// multisig signatures must have a constant fee
				return 0 == transaction.getFee().compareTo(minimumFee);
		}

		return transaction.getFee().compareTo(minimumFee) >= 0;
	}
}

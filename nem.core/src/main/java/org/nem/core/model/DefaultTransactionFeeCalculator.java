package org.nem.core.model;

import org.nem.core.model.primitive.*;

/**
 * Default implementation for calculating and validating transaction fees.
 */
public class DefaultTransactionFeeCalculator implements TransactionFeeCalculator {
	private static final Amount FEE_UNIT = Amount.fromNem(2);
	private static final long FEE_UNIT_NUM_NEM = FEE_UNIT.getNumNem();
	private static final int FEE_MULTIPLIER = 3;

	/**
	 * Calculates the minimum fee for the specified transaction at the specified block height.
	 *
	 * @param transaction The transaction.
	 * @return The minimum fee.
	 */
	@Override
	public Amount calculateMinimumFee(final Transaction transaction) {
		switch (transaction.getType()) {
			case TransactionTypes.TRANSFER:
				// TODO 20150715 J-*: should we charge more for transfers with mosaic transfers attached?
				// TODO 20150716 BR -> J: definitely should charge more depending on the size of the bag.
				return calculateMinimumFee((TransferTransaction)transaction);

			case TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION:
				return calculateMinimumFee((MultisigAggregateModificationTransaction)transaction);
			case TransactionTypes.PROVISION_NAMESPACE:
				return FEE_UNIT.multiply(FEE_MULTIPLIER).multiply(18);
			case TransactionTypes.MOSAIC_SUPPLY_CHANGE:
				// TODO 20150710 BR -> all: how much fees should a supply transaction have?
				// > should a mosaic creation transaction really only have 6 xem fee?
				return FEE_UNIT.multiply(FEE_MULTIPLIER).multiply(18);
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
	@Override
	public boolean isFeeValid(final Transaction transaction, final BlockHeight blockHeight) {
		final Amount minimumFee = this.calculateMinimumFee(transaction);
		final long FORK_HEIGHT = 92000;
		final Amount maxCacheFee = Amount.fromNem(1000); // 1000 xem is the maximum fee that helps push a transaction into the cache
		switch (transaction.getType()) {
			case TransactionTypes.MULTISIG_SIGNATURE:
				if (FORK_HEIGHT > blockHeight.getRaw()) {
					// multisig signatures must have a constant fee
					return 0 == transaction.getFee().compareTo(minimumFee);
				}

				// minimumFee <= multisig signatures fee <= 1000
				// reason: during spam attack cosignatories must be able to get their signature into the cache.
				//         it is limited in order for the last cosignatory not to be able to drain the multisig account
				return 0 <= transaction.getFee().compareTo(minimumFee) && 0 >= transaction.getFee().compareTo(maxCacheFee);
		}

		return transaction.getFee().compareTo(minimumFee) >= 0;
	}
}

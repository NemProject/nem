package org.nem.core.model;

import org.nem.core.model.mosaic.*;
import org.nem.core.utils.SetOnce;

/**
 * Helper class for storing NEM globals that can be accessed by other core classes.
 * <br>
 * This class should really be used sparingly!
 */
public class NemGlobals {
	private static final SetOnce<TransactionFeeCalculator> TRANSACTION_FEE_CALCULATOR =
			new SetOnce<>(new DefaultTransactionFeeCalculator());

	private static final SetOnce<MosaicTransferFeeCalculator> MOSAIC_TRANSFER_FEE_CALCULATOR =
			new SetOnce<>(new DefaultMosaicTransferFeeCalculator());

	private static final SetOnce<BlockChainConfiguration> BLOCK_CHAIN_CONFIGURATION =
			new SetOnce<>(BlockChainConfiguration.defaultConfiguration());

	/**
	 * Gets the global transaction fee calculator.
	 *
	 * @return The transaction fee calculator.
	 */
	public static TransactionFeeCalculator getTransactionFeeCalculator() {
		return TRANSACTION_FEE_CALCULATOR.get();
	}

	/**
	 * Sets the global transaction fee calculator.
	 *
	 * @param calculator The transaction fee calculator.
	 */
	public static void setTransactionFeeCalculator(final TransactionFeeCalculator calculator) {
		TRANSACTION_FEE_CALCULATOR.set(calculator);
	}

	/**
	 * Gets the global mosaic transfer fee calculator.
	 *
	 * @return The mosaic transfer fee calculator.
	 */
	public static MosaicTransferFeeCalculator getMosaicTransferFeeCalculator() {
		return MOSAIC_TRANSFER_FEE_CALCULATOR.get();
	}

	/**
	 * Sets the global mosaic transfer fee calculator.
	 *
	 * @param calculator The mosaic transfer fee calculator.
	 */
	public static void setMosaicTransferFeeCalculator(final MosaicTransferFeeCalculator calculator) {
		MOSAIC_TRANSFER_FEE_CALCULATOR.set(calculator);
	}

	/**
	 * Gets the global block chain configuration.
	 *
	 * @return The block chain configuration.
	 */
	public static BlockChainConfiguration getBlockChainConfiguration() {
		return BLOCK_CHAIN_CONFIGURATION.get();
	}

	/**
	 * Sets the global block chain configuration.
	 *
	 * @param configuration The block chain configuration.
	 */
	public static void setBlockChainConfiguration(final BlockChainConfiguration configuration) {
		BLOCK_CHAIN_CONFIGURATION.set(configuration);
	}
}

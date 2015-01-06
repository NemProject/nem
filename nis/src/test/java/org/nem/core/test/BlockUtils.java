package org.nem.core.test;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;

/**
 * Static class containing test utilities for creating blocks and transactions.
 */
public class BlockUtils {
	/**
	 * The previous hash used in created blocks.
	 */
	public static final Hash DUMMY_PREVIOUS_HASH = Utils.generateRandomHash();

	/**
	 * The generation hash used in created blocks.
	 */
	public static final Hash DUMMY_GENERATION_HASH = Utils.generateRandomHash();

	/**
	 * Creates a transaction with the specified fee.
	 *
	 * @param fee The fee.
	 * @return The transaction.
	 */
	public static MockTransaction createTransactionWithFee(final long fee) {
		// Arrange:
		return createTransactionWithFee(127, fee);
	}

	/**
	 * Creates a transaction with the specified custom field and fee.
	 *
	 * @param customField The custom field.
	 * @param fee The fee.
	 * @return The transaction.
	 */
	public static MockTransaction createTransactionWithFee(final int customField, final long fee) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(sender, customField);
		transaction.setFee(new Amount(fee));
		return transaction;
	}

	/**
	 * Creates a block with the specified harvester account.
	 *
	 * @param harvester The harvester.
	 * @return The block.
	 */
	public static Block createBlock(final Account harvester) {
		// Arrange:
		return new Block(harvester, DUMMY_PREVIOUS_HASH, DUMMY_GENERATION_HASH, new TimeInstant(7), new BlockHeight(3));
	}

	/**
	 * Creates a block with the specified height.
	 *
	 * @param height The height.
	 * @return The block.
	 */
	public static Block createBlockWithHeight(final BlockHeight height) {
		// Arrange:
		final Account harvester = Utils.generateRandomAccount();
		return new Block(
				harvester,
				DUMMY_PREVIOUS_HASH,
				DUMMY_GENERATION_HASH,
				new TimeInstant(7),
				height);
	}

	/**
	 * Creates a block with a random harvester.
	 *
	 * @return The block.
	 */
	public static Block createBlock() {
		// Arrange:
		return createBlock(Utils.generateRandomAccount());
	}
}

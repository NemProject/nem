package org.nem.nis.validators.integration;

import org.nem.core.model.*;
import org.nem.nis.test.NisUtils;

import java.util.List;

public class SingleBlockBlockChainValidatorTransactionValidationTest extends AbstractBlockChainValidatorTransactionValidationTest {

	@Override
	protected List<Block> getBlocks(final Block parentBlock, final List<Transaction> transactions) {
		// put all the transactions in a single block
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
		blocks.get(0).addTransactions(transactions);
		return blocks;
	}
}

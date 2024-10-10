package org.nem.nis.validators.integration;

import java.util.List;
import org.nem.core.model.*;
import org.nem.nis.test.NisUtils;

public class SingleBlockBlockChainValidatorTransactionValidationTest extends AbstractBlockChainValidatorTransactionValidationTest {

	@Override
	protected List<Block> getBlocks(final Block parentBlock, final List<Transaction> transactions) {
		// put all the transactions in a single block
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
		blocks.get(0).addTransactions(transactions);
		return blocks;
	}
}

package org.nem.nis.validators.integration;

import org.nem.core.model.*;
import org.nem.nis.test.NisUtils;

import java.util.List;

public class MultiBlockBlockChainValidatorTransactionValidationTest extends AbstractBlockChainValidatorTransactionValidationTest {

	@Override
	protected List<Block> getBlocks(final Block parentBlock, final List<Transaction> transactions) {
		// put each transaction in a separate block
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, transactions.size());
		for (int i = 0; i < blocks.size(); ++i) {
			blocks.get(i).addTransaction(transactions.get(i));
		}
		return blocks;
	}

	@Override
	protected boolean isSingleBlockUsed() {
		return false;
	}
}

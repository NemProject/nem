package org.nem.nis.test.validation;

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

	@Override
	protected ValidationResult getHashConflictResult() {
		// TODO 20150203 J-B: does it make sense that this is different across blocks and within blocks?
		// TODO 20150304 BR -> J: I would prefer FAILURE_TRANSACTION_DUPLICATE in all places except UnconfirmedTransactions.addNew()
		// > since it is common that we try to add transactions we already know. That produces to much trash in the log.
		// > (or does the transaction cache in the push service prevent that?)
		return ValidationResult.NEUTRAL;
	}
}

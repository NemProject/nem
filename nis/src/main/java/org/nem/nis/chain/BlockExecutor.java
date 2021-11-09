package org.nem.nis.chain;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.BlockTransactionObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for executing blocks.
 */
@Service
public class BlockExecutor {
	private final ReadOnlyNisCache nisCache;

	/**
	 * Creates a new block executor.
	 *
	 * @param nisCache The NIS cache.
	 */
	@Autowired(required = true)
	public BlockExecutor(final ReadOnlyNisCache nisCache) {
		this.nisCache = nisCache;
	}

	// region execute

	/**
	 * Executes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void execute(final Block block, final BlockTransactionObserver observer) {
		final BlockProcessor processor = new BlockExecuteProcessor(this.nisCache, block, observer);
		block.getTransactions().forEach(processor::process);
		processor.process();
	}

	// endregion

	// region undo

	/**
	 * Undoes all transactions in the block with a custom observer.
	 *
	 * @param block The block.
	 * @param observer The observer.
	 */
	public void undo(final Block block, final BlockTransactionObserver observer) {
		final BlockProcessor processor = new BlockUndoProcessor(this.nisCache, block, observer);
		processor.process();
		getReverseTransactions(block).forEach(processor::process);
	}

	// endregion

	private static Iterable<Transaction> getReverseTransactions(final Block block) {
		return () -> new ReverseListIterator<>(block.getTransactions());
	}
}

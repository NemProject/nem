package org.nem.nis.chain;

import org.nem.core.model.*;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;

/**
 * Block processor for executing blocks.
 */
public class BlockExecuteProcessor extends AbstractBlockProcessor {
	private final TransactionExecutionState state;

	/**
	 * Creates a new block processor.
	 *
	 * @param nisCache The NIS cache.
	 * @param block The block.
	 * @param observer The block observer.
	 */
	public BlockExecuteProcessor(final ReadOnlyNisCache nisCache, final Block block, final BlockTransactionObserver observer) {
		super(nisCache, block, observer, NotificationTrigger.Execute);
		this.state = NisCacheUtils.createTransactionExecutionState(nisCache);
	}

	@Override
	public void process() {
		super.notifyBlockHarvested();
		super.notifyTransactionHashes();
	}

	@Override
	public void process(final Transaction transaction) {
		transaction.execute(this.getObserver(), this.state);
	}
}

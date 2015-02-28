package org.nem.nis.chain;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;

/**
 * Block processor for undoing blocks.
 */
public class BlockUndoProcessor extends AbstractBlockProcessor {

	/**
	 * Creates a new block processor.
	 *
	 * @param nisCache The NIS cache.
	 * @param block The block.
	 * @param observer The block observer.
	 */
	public BlockUndoProcessor(final ReadOnlyNisCache nisCache, final Block block, final BlockTransactionObserver observer) {
		super(nisCache, block, observer, NotificationTrigger.Undo);
	}

	@Override
	public void process() {
		super.notifyBlockHarvested();
		super.notifyTransactionHashes();
	}

	@Override
	public void process(final Transaction transaction) {
		transaction.undo(this.getObserver());
	}
}

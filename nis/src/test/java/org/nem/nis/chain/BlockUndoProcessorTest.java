package org.nem.nis.chain;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.*;

public class BlockUndoProcessorTest extends AbstractBlockProcessorTest {

	@Override
	protected NotificationTrigger getTrigger() {
		return NotificationTrigger.Undo;
	}

	@Override
	protected boolean supportsTransactionExecution() {
		return true;
	}

	@Override
	protected void process(final Block block, final ReadOnlyNisCache nisCache, final BlockTransactionObserver observer) {
		final BlockProcessor processor = new BlockUndoProcessor(nisCache, block, observer);
		processor.process();
	}

	@Override
	protected void process(final Transaction transaction, final Block block, final ReadOnlyNisCache nisCache,
			final BlockTransactionObserver observer) {
		final BlockProcessor processor = new BlockUndoProcessor(nisCache, block, observer);
		processor.process(transaction);
	}
}

package org.nem.nis.secret.pruning;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.NisCache;
import org.nem.nis.secret.BlockTransactionObserver;

public class TransactionHashCachePruningObserverTest extends AbstractPruningObserverTest {

	// region overrides

	@Override
	protected BlockTransactionObserver createObserver(final NisCache nisCache) {
		return new TransactionHashCachePruningObserver(nisCache.getTransactionHashCache());
	}

	@Override
	protected void assertPruning(final NisCache nisCache, final long state) {
		Mockito.verify(nisCache.getTransactionHashCache(), Mockito.only()).prune(new TimeInstant((int) state));
	}

	@Override
	protected void assertNoPruning(final NisCache nisCache) {
		Mockito.verify(nisCache.getTransactionHashCache(), Mockito.never()).prune(Mockito.any());
	}

	// endregion

	@Test
	public void timeBasedPruningIsTriggeredAtInitialTime() {
		// Assert:
		this.assertTimeBasedPruning(TimeInstant.ZERO, 0);
	}

	@Test
	public void timeBasedPruningIsTriggeredAtAllTimes() {
		// Arrange:
		final int RETENTION_SECONDS = RETENTION_HOURS * 60 * 60;

		// Assert: state is expected prune timestamp
		final TimeInstant relativeTime1 = TimeInstant.ZERO.addHours(RETENTION_HOURS);
		this.assertTimeBasedPruning(relativeTime1.addSeconds(-1), RETENTION_SECONDS - 1);
		this.assertTimeBasedPruning(relativeTime1, RETENTION_SECONDS);
		this.assertTimeBasedPruning(relativeTime1.addSeconds(1), RETENTION_SECONDS + 1);

		final TimeInstant relativeTime2 = TimeInstant.ZERO.addHours(2 * RETENTION_HOURS);
		this.assertTimeBasedPruning(relativeTime2.addSeconds(-1), 2 * RETENTION_SECONDS - 1);
		this.assertTimeBasedPruning(relativeTime2, 2 * RETENTION_SECONDS);
		this.assertTimeBasedPruning(relativeTime2.addSeconds(1), 2 * RETENTION_SECONDS + 1);

		final TimeInstant relativeTime3 = TimeInstant.ZERO.addHours(3 * RETENTION_HOURS);
		this.assertTimeBasedPruning(relativeTime3.addSeconds(-1), 3 * RETENTION_SECONDS - 1);
		this.assertTimeBasedPruning(relativeTime3, 3 * RETENTION_SECONDS);
		this.assertTimeBasedPruning(relativeTime3.addSeconds(1), 3 * RETENTION_SECONDS + 1);
	}

	// endregion
}

package org.nem.nis.secret;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.observers.TransactionHashesNotification;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;

import java.util.*;

public class TransactionHashesObserverTest {

	// region execute

	@Test
	public void transactionHashesExecutePutsPairsIntoTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifyTransactionHashes(context.observer, context.pairs, NotificationTrigger.Execute);

		// Assert:
		MatcherAssert.assertThat(context.transactionHashCache.size(), IsEqual.equalTo(10));
		for (final HashMetaDataPair pair : context.pairs) {
			MatcherAssert.assertThat(context.transactionHashCache.get(pair.getHash()), IsEqual.equalTo(pair.getMetaData()));
		}
	}

	// endregion

	// region undo

	@Test
	public void transactionHashesUndoRemovesPairsFromTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();
		context.transactionHashCache.putAll(context.pairs);

		// Act:
		notifyTransactionHashes(context.observer, context.pairs, NotificationTrigger.Undo);

		// Assert:
		MatcherAssert.assertThat(context.transactionHashCache.size(), IsEqual.equalTo(0));
	}

	// endregion

	private static void notifyTransactionHashes(final TransactionHashesObserver observer, final List<HashMetaDataPair> pairs,
			final NotificationTrigger trigger) {
		observer.notify(new TransactionHashesNotification(pairs),
				new BlockNotificationContext(new BlockHeight(4), new TimeInstant(123), trigger));
	}

	private class TestContext {
		private final List<HashMetaDataPair> pairs = this.createPairs();
		private final HashCache transactionHashCache;
		private final TransactionHashesObserver observer;

		private TestContext() {
			this.transactionHashCache = new DefaultHashCache();
			this.observer = new TransactionHashesObserver(this.transactionHashCache);
		}

		private List<HashMetaDataPair> createPairs() {
			final List<HashMetaDataPair> pairs = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				pairs.add(new HashMetaDataPair(Utils.generateRandomHash(),
						new HashMetaData(new BlockHeight(12), Utils.generateRandomTimeStamp())));
			}

			return pairs;
		}
	}
}

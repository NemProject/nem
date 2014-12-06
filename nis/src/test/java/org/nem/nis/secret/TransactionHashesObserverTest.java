package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.observers.TransactionHashesNotification;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

public class TransactionHashesObserverTest {

	//region execute

	@Test
	public void transactionHashesExecutePutsPairsIntoTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		notifyTransactionHashes(context.observer, context.pairs, NotificationTrigger.Execute);

		// Assert:
		Assert.assertThat(context.transactionHashCache.size(), IsEqual.equalTo(10));
		Assert.assertThat(
				context.transactionHashCache.stream().map(e -> new HashMetaDataPair(e.getKey(), e.getValue())).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(context.pairs));
	}

	//endregion

	//region undo

	@Test
	public void transactionHashesUndoRemovesPairsFromTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();
		context.transactionHashCache.putAll(context.pairs);

		// Act:
		notifyTransactionHashes(context.observer, context.pairs, NotificationTrigger.Undo);

		// Assert:
		Assert.assertThat(context.transactionHashCache.isEmpty(), IsEqual.equalTo(true));
	}

	//endregion

	private static void notifyTransactionHashes(
			final TransactionHashesObserver observer,
			final List<HashMetaDataPair> pairs,
			final NotificationTrigger trigger) {
		observer.notify(
				new TransactionHashesNotification(pairs),
				new BlockNotificationContext(new BlockHeight(4), new TimeInstant(123), trigger));
	}

	private class TestContext {
		private final List<HashMetaDataPair> pairs = this.createPairs();
		private final HashCache transactionHashCache;
		private final TransactionHashesObserver observer;

		private TestContext() {
			this.transactionHashCache = new HashCache();
			this.observer = new TransactionHashesObserver(transactionHashCache);
		}

		private List<HashMetaDataPair> createPairs() {
			final List<HashMetaDataPair> pairs = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				pairs.add(new HashMetaDataPair(Utils.generateRandomHash(), new HashMetaData(new BlockHeight(12), Utils.generateRandomTimeStamp())));
			}

			return pairs;
		}
	}
}

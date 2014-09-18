package org.nem.nis.secret;

import org.nem.core.model.observers.Notification;

import java.util.*;

/**
 * Builder for building an aggregate BlockTransactionObserver.
 */
public class AggregateBlockTransactionObserverBuilder {
	private final List<BlockTransactionObserver> observers = new ArrayList<>();

	/**
	 * Adds an observer to the aggregate.
	 *
	 * @param observer The observer to add.
	 */
	public void add(final BlockTransactionObserver observer) {
		this.observers.add(observer);
	}

	/**
	 * Adds an observer to the aggregate.
	 *
	 * @param observer The observer to add.
	 */
	public void add(final BlockTransferObserver observer) {
		this.observers.add(new BlockTransferObserverToBlockTransactionObserverAdapter(observer));
	}

	/**
	 * Builds the aggregate observer.
	 *
	 * @return the aggregate observer.
	 */
	public BlockTransactionObserver build() {
		return new AggregateBlockTransactionObserver(this.observers);
	}

	private static class AggregateBlockTransactionObserver implements BlockTransactionObserver {
		private final List<BlockTransactionObserver> observers;

		public AggregateBlockTransactionObserver(final List<BlockTransactionObserver> observers) {
			this.observers = observers;
		}

		@Override
		public void notify(final Notification notification, final BlockNotificationContext context) {
			for (final BlockTransactionObserver observer : this.observers) {
				observer.notify(notification, context);
			}
		}
	}
}

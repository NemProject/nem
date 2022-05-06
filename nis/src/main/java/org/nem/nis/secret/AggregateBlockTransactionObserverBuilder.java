package org.nem.nis.secret;

import org.nem.core.model.observers.*;

import java.util.*;
import java.util.stream.Collectors;

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
	 * Adds an observer to the aggregate.
	 *
	 * @param observer The observer to add.
	 */
	public void add(final TransactionObserver observer) {
		this.add(new TransactionObserverToBlockTransferObserverAdapter(observer));
	}

	/**
	 * Builds the aggregate observer by chaining all observers.
	 *
	 * @return The aggregate observer.
	 */
	public BlockTransactionObserver build() {
		return new AggregateBlockTransactionObserver(this.observers);
	}

	/**
	 * Builds the aggregate observer by chaining all observers in reverse order.
	 *
	 * @return The aggregate observer.
	 */
	public BlockTransactionObserver buildReverse() {
		Collections.reverse(this.observers);
		return new AggregateBlockTransactionObserver(this.observers);
	}

	private static class AggregateBlockTransactionObserver implements BlockTransactionObserver {
		private final List<BlockTransactionObserver> observers;

		public AggregateBlockTransactionObserver(final List<BlockTransactionObserver> observers) {
			this.observers = observers;
		}

		@Override
		public String getName() {
			return this.observers.stream().map(NamedObserver::getName).collect(Collectors.joining(","));
		}

		@Override
		public void notify(final Notification notification, final BlockNotificationContext context) {
			for (final BlockTransactionObserver observer : this.observers) {
				observer.notify(notification, context);
			}
		}
	}
}

package org.nem.nis.secret;

import org.nem.core.model.observers.*;

import java.util.*;

/**
 * Builder for building an aggregate TransactionObserver.
 */
public class AggregateTransactionObserverBuilder {
	private final List<TransactionObserver> observers = new ArrayList<>();

	/**
	 * Adds an observer to the aggregate.
	 *
	 * @param observer The observer to add.
	 */
	public void add(final TransactionObserver observer) {
		this.observers.add(observer);
	}

	/**
	 * Builds the aggregate observer.
	 *
	 * @return the aggregate observer.
	 */
	public TransactionObserver build() {
		return new AggregateTransactionObserver(this.observers);
	}

	private static class AggregateTransactionObserver implements TransactionObserver {
		private final List<TransactionObserver> observers;

		public AggregateTransactionObserver(final List<TransactionObserver> observers) {
			this.observers = observers;
		}

		@Override
		public void notify(final Notification notification) {
			for (final TransactionObserver observer : this.observers) {
				observer.notify(notification);
			}
		}
	}
}

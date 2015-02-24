package org.nem.core.test;

import org.nem.core.model.Transaction;

import java.util.*;
import java.util.stream.*;

/**
 * Static class containing helper functions for dealing with mock transactions.
 */
public class MockTransactionUtils {

	/**
	 * Given a stream of mock transactions, returns a list of the corresponding custom fields.
	 *
	 * @param stream The stream of mock transactions.
	 * @return The custom fields.
	 */
	public static List<Integer> getCustomFields(final Stream<Transaction> stream) {
		return stream
				.map(t -> ((MockTransaction)t).getCustomField())
				.collect(Collectors.toList());
	}

	/**
	 * Given a collection of mock transactions, returns a list of the corresponding custom fields.
	 *
	 * @param transactions The transactions.
	 * @return The custom fields.
	 */
	public static List<Integer> getCustomFieldValues(final Collection<Transaction> transactions) {
		return getCustomFields(transactions.stream());
	}

	/**
	 * Creates a mock transaction with nested children.
	 * Root - seed
	 * Level 1 - seed + 10, seed + 20, seed + 30
	 * Level 2 - seed + 11, seed + 12, seed + 31, seed + 32
	 * .
	 *
	 * @param seed The seed custom field.
	 * @return The transaction
	 */
	public static Transaction createMockTransactionWithNestedChildren(final int seed) {
		final Transaction child1 = createMockTransactionWithTwoChildren(seed + 10);
		final Transaction child2 = new MockTransaction(Utils.generateRandomAccount(), seed + 20);
		final Transaction child3 = createMockTransactionWithTwoChildren(seed + 30);

		final MockTransaction parent = new MockTransaction(Utils.generateRandomAccount(), seed);
		parent.setChildTransactions(Arrays.asList(child1, child2, child3));
		return parent;
	}

	private static Transaction createMockTransactionWithTwoChildren(final int seed) {
		final MockTransaction parent = new MockTransaction(Utils.generateRandomAccount(), seed);
		final MockTransaction child1 = new MockTransaction(Utils.generateRandomAccount(), seed + 1);
		final MockTransaction child2 = new MockTransaction(Utils.generateRandomAccount(), seed + 2);
		parent.setChildTransactions(Arrays.asList(child1, child2));
		return parent;
	}
}

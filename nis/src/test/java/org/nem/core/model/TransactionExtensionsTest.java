package org.nem.core.model;

import org.junit.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.*;

public class TransactionExtensionsTest {

	@Test
	public void canStreamSelfAndFirstChildTransactions() {
		// Arrange:
		final Transaction transaction = createTestTransaction();

		// Act:
		final List<Integer> customFields = getCustomFields(TransactionExtensions.streamSelfAndFirstChildTransactions(transaction));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 60, 70, 80));
	}

	@Test
	public void canStreamSelfAndAllTransactions() {
		// Arrange:
		final Transaction transaction = createTestTransaction();

		// Act:
		final List<Integer> customFields = getCustomFields(TransactionExtensions.streamSelfAndAllTransactions(transaction));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 60, 61, 62, 70, 80, 81, 82));
	}

	@Test
	public void canStreamDefaultTransactions() {
		// Arrange:
		final Transaction transaction = createTestTransaction();

		// Act:
		final List<Integer> customFields = getCustomFields(TransactionExtensions.streamDefault(transaction));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 60, 70, 80));
	}

	private static List<Integer> getCustomFields(final Stream<Transaction> stream) {
		return MockTransactionUtils.getCustomFields(stream);
	}

	private static Transaction createTestTransaction() {
		return MockTransactionUtils.createMockTransactionWithNestedChildren(50);
	}
}
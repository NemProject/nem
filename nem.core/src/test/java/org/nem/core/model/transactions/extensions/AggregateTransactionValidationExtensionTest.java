package org.nem.core.model.transactions.extensions;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Transaction;
import org.nem.core.test.RandomTransactionFactory;

import java.util.*;

public class AggregateTransactionValidationExtensionTest {

	@Test
	public void aggregateCallsApplicableChildExtension() {
		// Arrange:
		final TransactionValidationExtension<Transaction> extension = createMockExtension(true);
		final AggregateTransactionValidationExtension<Transaction> aggregate =
				new AggregateTransactionValidationExtension<>(Collections.singletonList(extension));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.validate(transaction);

		// Assert:
		Mockito.verify(extension, Mockito.times(1)).isApplicable(2);
		Mockito.verify(extension, Mockito.times(1)).validate(transaction);
	}

	@Test
	public void aggregateDoesNotCallNonApplicableChildExtension() {
		// Arrange:
		final TransactionValidationExtension<Transaction> extension = createMockExtension(false);
		final AggregateTransactionValidationExtension<Transaction> aggregate =
				new AggregateTransactionValidationExtension<>(Collections.singletonList(extension));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.validate(transaction);

		// Assert:
		Mockito.verify(extension, Mockito.times(1)).isApplicable(2);
		Mockito.verify(extension, Mockito.never()).validate(transaction);
	}

	@Test
	public void aggregateCallsAllApplicableChildExtension() {
		// Arrange:
		final TransactionValidationExtension<Transaction> extension1 = createMockExtension(true);
		final TransactionValidationExtension<Transaction> extension2 = createMockExtension(false);
		final TransactionValidationExtension<Transaction> extension3 = createMockExtension(true);
		final AggregateTransactionValidationExtension<Transaction> aggregate =
				new AggregateTransactionValidationExtension<>(Arrays.asList(extension1, extension2, extension3));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.validate(transaction);

		// Assert:
		Mockito.verify(extension1, Mockito.times(1)).validate(transaction);
		Mockito.verify(extension2, Mockito.never()).validate(transaction);
		Mockito.verify(extension3, Mockito.times(1)).validate(transaction);

	}

	@SuppressWarnings("unchecked")
	private static TransactionValidationExtension<Transaction> createMockExtension(final boolean isApplicable) {
		final TransactionValidationExtension<Transaction> extension = Mockito.mock(TransactionValidationExtension.class);
		Mockito.when(extension.isApplicable(Mockito.anyInt())).thenReturn(isApplicable);
		return extension;
	}
}
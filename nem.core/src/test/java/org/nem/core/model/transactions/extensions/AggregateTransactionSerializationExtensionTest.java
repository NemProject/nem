package org.nem.core.model.transactions.extensions;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Transaction;
import org.nem.core.serialization.*;
import org.nem.core.test.RandomTransactionFactory;

import java.util.*;

public class AggregateTransactionSerializationExtensionTest {

	//region serialize

	@Test
	public void serializeCallsApplicableChildExtension() {
		// Arrange:
		final Serializer serializer = Mockito.mock(Serializer.class);
		final TransactionSerializationExtension<Object> extension = createMockExtension(true);
		final AggregateTransactionSerializationExtension<Object> aggregate =
				new AggregateTransactionSerializationExtension<>(Collections.singletonList(extension));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.serialize(serializer, 7, transaction);

		// Assert:
		Mockito.verify(extension, Mockito.times(1)).isApplicable(7);
		Mockito.verify(extension, Mockito.times(1)).serialize(serializer, transaction);
	}

	@Test
	public void serializeDoesNotCallNonApplicableChildExtension() {
		// Arrange:
		final Serializer serializer = Mockito.mock(Serializer.class);
		final TransactionSerializationExtension<Object> extension = createMockExtension(false);
		final AggregateTransactionSerializationExtension<Object> aggregate =
				new AggregateTransactionSerializationExtension<>(Collections.singletonList(extension));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.serialize(serializer, 7, transaction);

		// Assert:
		Mockito.verify(extension, Mockito.times(1)).isApplicable(7);
		Mockito.verify(extension, Mockito.never()).serialize(Mockito.any(), Mockito.any());
	}

	@Test
	public void serializeCallsAllApplicableChildExtension() {
		// Arrange:
		final Serializer serializer = Mockito.mock(Serializer.class);
		final TransactionSerializationExtension<Object> extension1 = createMockExtension(true);
		final TransactionSerializationExtension<Object> extension2 = createMockExtension(false);
		final TransactionSerializationExtension<Object> extension3 = createMockExtension(true);
		final AggregateTransactionSerializationExtension<Object> aggregate =
				new AggregateTransactionSerializationExtension<>(Arrays.asList(extension1, extension2, extension3));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.serialize(serializer, 7, transaction);

		// Assert:
		Mockito.verify(extension1, Mockito.times(1)).serialize(serializer, transaction);
		Mockito.verify(extension2, Mockito.never()).serialize(Mockito.any(), Mockito.any());
		Mockito.verify(extension3, Mockito.times(1)).serialize(serializer, transaction);
	}

	//endregion

	//region deserialize

	@Test
	public void deserializeCallsApplicableChildExtension() {
		// Arrange:
		final Deserializer deserializer = Mockito.mock(Deserializer.class);
		final TransactionSerializationExtension<Object> extension = createMockExtension(true);
		final AggregateTransactionSerializationExtension<Object> aggregate =
				new AggregateTransactionSerializationExtension<>(Collections.singletonList(extension));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.deserialize(deserializer, 8, transaction);

		// Assert:
		Mockito.verify(extension, Mockito.times(1)).isApplicable(8);
		Mockito.verify(extension, Mockito.times(1)).deserialize(deserializer, transaction);
	}

	@Test
	public void deserializeDoesNotCallNonApplicableChildExtension() {
		// Arrange:
		final Deserializer deserializer = Mockito.mock(Deserializer.class);
		final TransactionSerializationExtension<Object> extension = createMockExtension(false);
		final AggregateTransactionSerializationExtension<Object> aggregate =
				new AggregateTransactionSerializationExtension<>(Collections.singletonList(extension));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.deserialize(deserializer, 8, transaction);

		// Assert:
		Mockito.verify(extension, Mockito.times(1)).isApplicable(8);
		Mockito.verify(extension, Mockito.never()).deserialize(Mockito.any(), Mockito.any());
	}

	@Test
	public void deserializeCallsAllApplicableChildExtension() {
		// Arrange:
		final Deserializer deserializer = Mockito.mock(Deserializer.class);
		final TransactionSerializationExtension<Object> extension1 = createMockExtension(true);
		final TransactionSerializationExtension<Object> extension2 = createMockExtension(false);
		final TransactionSerializationExtension<Object> extension3 = createMockExtension(true);
		final AggregateTransactionSerializationExtension<Object> aggregate =
				new AggregateTransactionSerializationExtension<>(Arrays.asList(extension1, extension2, extension3));

		// Act:
		final Transaction transaction = RandomTransactionFactory.createTransfer();
		aggregate.deserialize(deserializer, 8, transaction);

		// Assert:
		Mockito.verify(extension1, Mockito.times(1)).deserialize(deserializer, transaction);
		Mockito.verify(extension2, Mockito.never()).deserialize(Mockito.any(), Mockito.any());
		Mockito.verify(extension3, Mockito.times(1)).deserialize(deserializer, transaction);
	}

	//endregion

	@SuppressWarnings("unchecked")
	private static TransactionSerializationExtension<Object> createMockExtension(final boolean isApplicable) {
		final TransactionSerializationExtension<Object> extension = Mockito.mock(TransactionSerializationExtension.class);
		Mockito.when(extension.isApplicable(Mockito.anyInt())).thenReturn(isApplicable);
		return extension;
	}
}
package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;
import java.util.stream.*;

public class TransactionRegistryTest {

	@Test
	public void allExpectedTransactionTypesAreSupported() {
		// Assert:
		Assert.assertThat(TransactionRegistry.size(), IsEqual.equalTo(4));
	}

	@Test
	public void allExpectedMultisigEmbeddableTypesAreSupported() {
		// Assert:
		Assert.assertThat(TransactionRegistry.multisigEmbeddableSize(), IsEqual.equalTo(3));
	}

	@Test
	public void transactionRegistryIsSameSizeAsTransactionFactory() {
		// Assert:
		Assert.assertThat(TransactionRegistry.size(), IsEqual.equalTo(TransactionFactory.size()));
	}

	@Test
	public void allExpectedEntriesAreReturnedViaIterator() {
		// Act:
		final Collection<Class> modelClasses = StreamSupport.stream(TransactionRegistry.iterate().spliterator(), false)
				.map(e -> e.modelClass)
				.collect(Collectors.toList());

		// Assert:
		final Collection<Class> expectedModelClasses = Arrays.asList(
				TransferTransaction.class,
				ImportanceTransferTransaction.class,
				MultisigSignerModificationTransaction.class,
				MultisigTransaction.class);
		Assert.assertThat(modelClasses, IsEquivalent.equivalentTo(expectedModelClasses));
	}
}
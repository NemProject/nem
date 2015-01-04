package org.nem.nis.mappers;

import org.hamcrest.core.*;
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
	public void transactionRegistryIsConsistentWithTransactionFactory() {
		// Assert:
		// (the transaction factory includes transactions that are not stored directly in blocks,
		// so it is aware of more transactions than the registry)
		Assert.assertThat(TransactionRegistry.size(), IsEqual.equalTo(TransactionFactory.size() - 1));
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
		Assert.assertThat(expectedModelClasses.size(), IsEqual.equalTo(TransactionRegistry.size()));
	}

	@Test
	public void findByTypeCanReturnAllRegisteredTypes() {
		// Arrange:
		final List<Integer> expectedRegisteredTypes = Arrays.asList(
				TransactionTypes.TRANSFER,
				TransactionTypes.IMPORTANCE_TRANSFER,
				TransactionTypes.MULTISIG_SIGNER_MODIFY,
				TransactionTypes.MULTISIG);

		// Act:
		for (final Integer type : expectedRegisteredTypes) {
			// Act:
			final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(type);

			// Assert:
			Assert.assertThat(entry.type, IsEqual.equalTo(type));
		}

		Assert.assertThat(expectedRegisteredTypes.size(), IsEqual.equalTo(TransactionRegistry.size()));
	}

	@Test
	public void findByTypeReturnsNullForUnregisteredType() {
		// Act:
		final TransactionRegistry.Entry<?, ?> entry = TransactionRegistry.findByType(TransactionTypes.ASSET_ASK);

		// Assert:
		Assert.assertThat(entry, IsNull.nullValue());
	}
}
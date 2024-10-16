package org.nem.core.model;

import java.util.*;
import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.nem.core.test.IsEquivalent;

public class TransactionTypesTest {

	@Test
	public void getActiveTypesReturnsAllExpectedTypes() {
		// Arrange:
		final Collection<Integer> expectedTypes = Arrays.asList(TransactionTypes.TRANSFER, TransactionTypes.IMPORTANCE_TRANSFER,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, TransactionTypes.PROVISION_NAMESPACE,
				TransactionTypes.MOSAIC_DEFINITION_CREATION, TransactionTypes.MOSAIC_SUPPLY_CHANGE, TransactionTypes.MULTISIG,
				TransactionTypes.MULTISIG_SIGNATURE);

		// Act:
		final Collection<Integer> types = TransactionTypes.getActiveTypes();

		// Assert:
		MatcherAssert.assertThat(types, IsEquivalent.equivalentTo(expectedTypes));
	}

	@Test
	public void getBlockEmbeddableTypesReturnsAllExpectedTypes() {
		// Arrange:
		final Collection<Integer> expectedTypes = Arrays.asList(TransactionTypes.TRANSFER, TransactionTypes.IMPORTANCE_TRANSFER,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, TransactionTypes.PROVISION_NAMESPACE,
				TransactionTypes.MOSAIC_DEFINITION_CREATION, TransactionTypes.MOSAIC_SUPPLY_CHANGE, TransactionTypes.MULTISIG);

		// Act:
		final Collection<Integer> types = TransactionTypes.getBlockEmbeddableTypes();

		// Assert:
		MatcherAssert.assertThat(types, IsEquivalent.equivalentTo(expectedTypes));
	}

	@Test
	public void getMultisigEmbeddableTypesReturnsAllExpectedTypes() {
		// Arrange:
		final Collection<Integer> expectedTypes = Arrays.asList(TransactionTypes.TRANSFER, TransactionTypes.IMPORTANCE_TRANSFER,
				TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, TransactionTypes.PROVISION_NAMESPACE,
				TransactionTypes.MOSAIC_DEFINITION_CREATION, TransactionTypes.MOSAIC_SUPPLY_CHANGE);

		// Act:
		final Collection<Integer> types = TransactionTypes.getMultisigEmbeddableTypes();

		// Assert:
		MatcherAssert.assertThat(types, IsEquivalent.equivalentTo(expectedTypes));
	}
}

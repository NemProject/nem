package org.nem.nis.validators;

import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;

import java.util.*;

public class TransactionValidatorFactoryTest {

	//region single

	@Test
	public void createSingleAddsDesiredSingleValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = getCompleteSingleValidatorNames();

		// Act:
		final String name = factory.createSingle(Mockito.mock(ReadOnlyAccountStateCache.class)).getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	@Test
	public void createSingleBuilderAddsDesiredSingleValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = getCompleteSingleValidatorNames();

		// Act:
		final String name = factory.createSingleBuilder(Mockito.mock(ReadOnlyAccountStateCache.class)).build().getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	@Test
	public void createIncompleteSingleBuilderAddsDesiredSingleValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = getIncompleteSingleValidatorNames();

		// Act:
		final String name = factory.createIncompleteSingleBuilder(Mockito.mock(ReadOnlyAccountStateCache.class)).build().getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	private static Collection<String> getCompleteSingleValidatorNames() {
		final Collection<String> expectedClasses = getIncompleteSingleValidatorNames();
		expectedClasses.add("MultisigSignaturesPresentValidator");
		return expectedClasses;
	}

	private static Collection<String> getIncompleteSingleValidatorNames() {
		return new ArrayList<String>() {
			{
				this.add("UniversalTransactionValidator");
				this.add("TransactionNonFutureEntityValidator");
				this.add("NemesisSinkValidator");
				this.add("BalanceValidator");
				this.add("TransactionNetworkValidator");

				this.add("TransferTransactionValidator");
				this.add("ImportanceTransferTransactionValidator");
				this.add("RemoteNonOperationalValidator");

				this.add("MultisigNonOperationalValidator");
				this.add("MultisigTransactionSignerValidator");
				this.add("MaxCosignatoryValidator");
				this.add("MultisigAggregateModificationTransactionValidator");
			}
		};
	}

	//endregion

	//region batch

	@Test
	public void createBatchAddsDesiredBatchValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = Arrays.asList("BatchUniqueHashTransactionValidator");

		// Act:
		final String name = factory.createBatch(Mockito.mock(ReadOnlyHashCache.class)).getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	//endregion

	private static void assertAreEquivalent(final String name, final Collection<String> expectedSubValidatorNames) {
		// Act:
		final List<String> subValidatorNames = Arrays.asList(name.split(","));

		// Assert:
		Assert.assertThat(subValidatorNames, IsEquivalent.equivalentTo(expectedSubValidatorNames));
	}

	private static TransactionValidatorFactory createFactory() {
		return new TransactionValidatorFactory(Mockito.mock(TimeProvider.class));
	}
}
package org.nem.nis.validators;

import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.NetworkInfos;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;

public class TransactionValidatorFactoryTest {

	// region single

	@Test
	public void createSingleAddsDesiredSingleValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = getCompleteSingleValidatorNames();

		// Act:
		final String name = factory.createSingle(Mockito.mock(ReadOnlyNisCache.class)).getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	@Test
	public void createSingleBuilderAddsDesiredSingleValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = getCompleteSingleValidatorNames();

		// Act:
		final String name = factory.createSingleBuilder(Mockito.mock(ReadOnlyNisCache.class)).build().getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	@Test
	public void createIncompleteSingleBuilderAddsDesiredSingleValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = getIncompleteSingleValidatorNames();

		// Act:
		final String name = factory.createIncompleteSingleBuilder(Mockito.mock(ReadOnlyNisCache.class)).build().getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	private static Collection<String> getCompleteSingleValidatorNames() {
		final Collection<String> expectedClasses = getIncompleteSingleValidatorNames();
		expectedClasses.add("MultisigSignaturesPresentValidator");
		return expectedClasses;
	}

	@SuppressWarnings("serial")
	private static Collection<String> getIncompleteSingleValidatorNames() {
		return new ArrayList<String>() {
			{
				this.add("DeadlineValidator");
				this.add("MinimumFeeValidator");
				this.add("VersionTransactionValidator");
				this.add("TransactionNonFutureEntityValidator");
				this.add("NemesisSinkValidator");
				this.add("BalanceValidator");
				this.add("TransactionNetworkValidator");

				this.add("TransferTransactionValidator");
				this.add("ImportanceTransferTransactionValidator");
				this.add("RemoteNonOperationalValidator");

				this.add("MultisigNonOperationalValidator");
				this.add("MultisigTransactionSignerValidator");
				this.add("FeeSinkNonOperationalValidator");
				this.add("NumCosignatoryRangeValidator");
				this.add("MultisigCosignatoryModificationValidator");

				this.add("ProvisionNamespaceTransactionValidator");
				this.add("MosaicDefinitionCreationTransactionValidator");
				this.add("MosaicSupplyChangeTransactionValidator");
				this.add("MosaicBagValidator");
				this.add("MosaicBalanceValidator");
			}
		};
	}

	// endregion

	// region batch

	@Test
	public void createBatchAddsDesiredBatchValidators() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();
		final Collection<String> expectedSubValidatorNames = Collections.singletonList("BatchUniqueHashTransactionValidator");

		// Act:
		final String name = factory.createBatch(Mockito.mock(ReadOnlyHashCache.class)).getName();

		// Assert:
		assertAreEquivalent(name, expectedSubValidatorNames);
	}

	// endregion

	private static void assertAreEquivalent(final String name, final Collection<String> expectedSubValidatorNames) {
		// Act:
		final List<String> subValidatorNames = Arrays.asList(name.split(","));

		// Assert:
		MatcherAssert.assertThat(subValidatorNames, IsEquivalent.equivalentTo(expectedSubValidatorNames));
	}

	private static TransactionValidatorFactory createFactory() {
		return new TransactionValidatorFactory(Mockito.mock(TimeProvider.class), NetworkInfos.getDefault(), new ForkConfiguration(), false);
	}
}

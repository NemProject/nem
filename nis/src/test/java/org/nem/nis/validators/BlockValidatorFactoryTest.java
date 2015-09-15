package org.nem.nis.validators;

import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.BlockChainConstants;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.ReadOnlyNisCache;

import java.util.*;

public class BlockValidatorFactoryTest {
	private static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = BlockChainConstants.DEFAULT_MAX_ALLOWED_TRANSACTIONS_PER_BLOCK;

	@Test
	public void createAddsDesiredBlockValidators() {
		// Arrange:
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class), MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
		final List<String> expectedSubValidatorNames = Arrays.asList(
				"TransactionDeadlineBlockValidator",
				"BlockNonFutureEntityValidator",
				"EligibleSignerBlockValidator",
				"MaxTransactionsBlockValidator",
				"NoSelfSignedTransactionsBlockValidator",
				"BlockUniqueHashTransactionValidator",
				"BlockMultisigAggregateModificationValidator",
				"BlockNetworkValidator",
				"BlockMosaicDefinitionCreationValidator",
				"VersionBlockValidator");

		// Act:
		final String name = factory.create(Mockito.mock(ReadOnlyNisCache.class)).getName();
		final List<String> subValidatorNames = Arrays.asList(name.split(","));

		// Assert:
		Assert.assertThat(subValidatorNames, IsEquivalent.equivalentTo(expectedSubValidatorNames));
	}

	@Test
	public void createTransactionOnlyAddsDesiredBlockValidators() {
		// Arrange:
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class), MAX_ALLOWED_TRANSACTIONS_PER_BLOCK);
		final List<String> expectedSubValidatorNames = Collections.singletonList(
				"BlockMultisigAggregateModificationValidator");

		// Act:
		final String name = factory.createTransactionOnly(Mockito.mock(ReadOnlyNisCache.class)).getName();
		final List<String> subValidatorNames = Arrays.asList(name.split(","));

		// Assert:
		Assert.assertThat(subValidatorNames, IsEquivalent.equivalentTo(expectedSubValidatorNames));
	}
}
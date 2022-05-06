package org.nem.nis.validators;

import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class BlockValidatorFactoryTest {

	@Test
	public void createAddsDesiredBlockValidators() {
		// Arrange:
		final BlockValidatorFactory factory = createFactory();
		final List<String> expectedSubValidatorNames = Arrays.asList("TransactionDeadlineBlockValidator", "BlockNonFutureEntityValidator",
				"EligibleSignerBlockValidator", "MaxTransactionsBlockValidator", "NoSelfSignedTransactionsBlockValidator",
				"BlockUniqueHashTransactionValidator", "BlockNetworkValidator", "VersionBlockValidator",
				"TreasuryReissuanceForkTransactionBlockValidator", "BlockMultisigAggregateModificationValidator",
				"BlockMosaicDefinitionCreationValidator");

		// Act:
		final String name = factory.create(Mockito.mock(ReadOnlyNisCache.class)).getName();
		final List<String> subValidatorNames = Arrays.asList(name.split(","));

		// Assert:
		MatcherAssert.assertThat(subValidatorNames, IsEquivalent.equivalentTo(expectedSubValidatorNames));
	}

	@Test
	public void createTransactionOnlyAddsDesiredBlockValidators() {
		// Arrange:
		final BlockValidatorFactory factory = createFactory();
		final List<String> expectedSubValidatorNames = Arrays.asList("BlockMultisigAggregateModificationValidator",
				"BlockMosaicDefinitionCreationValidator");

		// Act:
		final String name = factory.createTransactionOnly().getName();
		final List<String> subValidatorNames = Arrays.asList(name.split(","));

		// Assert:
		MatcherAssert.assertThat(subValidatorNames, IsEquivalent.equivalentTo(expectedSubValidatorNames));
	}

	private static BlockValidatorFactory createFactory() {
		return NisUtils.createBlockValidatorFactory();
	}
}

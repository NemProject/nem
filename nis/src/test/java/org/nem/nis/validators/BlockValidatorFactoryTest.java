package org.nem.nis.validators;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;

import java.util.*;
import java.util.stream.Collectors;

public class BlockValidatorFactoryTest {

	@Test
	public void createAddsDesiredBlockValidators() {
		// Arrange:
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class));
		final List<String> expectedSubValidatorNames = Arrays.asList(
				"TransactionDeadlineBlockValidator",
				"BlockNonFutureEntityValidator",
				"EligibleSignerBlockValidator",
				"MaxTransactionsBlockValidator",
				"NoSelfSignedTransactionsBlockValidator",
				"BlockImportanceTransferValidator",
				"BlockImportanceTransferBalanceValidator",
				"BlockUniqueHashTransactionValidator",
				"BlockMultisigAggregateModificationValidator");

		// Act:
		final String name = factory.create(Mockito.mock(ReadOnlyNisCache.class)).getName();
		final List<String> subValidatorNames = Arrays.asList(name.split(","));

		// Assert:
		Assert.assertThat(subValidatorNames, IsEquivalent.equivalentTo(expectedSubValidatorNames));
	}
}
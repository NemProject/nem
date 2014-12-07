package org.nem.nis.validators;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.core.time.TimeProvider;
import org.nem.nis.NisCache;

import java.util.*;
import java.util.stream.Collectors;

public class BlockValidatorFactoryTest {

	@Test
	public void createReturnsValidValidator() {
		// Arrange:
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class));

		// Act:
		final BlockValidator validator = factory.create(Mockito.mock(NisCache.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	@Test
	public void createAddsDesiredBlockValidators() {
		// Arrange:
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class));
		final List<Class<?>> classes = Arrays.asList(
				TransactionDeadlineBlockValidator.class,
				NonFutureEntityValidator.class,
				EligibleSignerBlockValidator.class,
				MaxTransactionsBlockValidator.class,
				NoSelfSignedTransactionsBlockValidator.class,
				BlockImportanceTransferValidator.class,
				BlockImportanceTransferBalanceValidator.class,
				BlockUniqueHashTransactionValidator.class);

		// Act:
		final List<BlockValidator> validators = new ArrayList<>();
		factory.visitSubValidators(validators::add, Mockito.mock(NisCache.class));

		// Assert:
		Assert.assertThat(
				validators.stream().map(BlockValidator::getClass).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(classes));
	}
}
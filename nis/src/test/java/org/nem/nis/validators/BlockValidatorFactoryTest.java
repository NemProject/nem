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

	// TODO 20141201 J-B: nice tests; you might want to add something similar to the transaction factory tests
	// TODO 20141204 BR -> J: the TransactionValidatorFactory doesn't expose the builder to the public. You have an idea how to do it?
	// TODO 20141205: J-B: i guess we would need to do something similar; also, we might want to consider having a single test
	// > that validates against all types, i'm not sure if there's much value in splitting up the tests
	// TODO 20141207 BR -> J: hope it's like you want it.
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
		Assert.assertThat(validators.stream().map(BlockValidator::getClass).collect(Collectors.toList()), IsEquivalent.equivalentTo(classes));
	}
}
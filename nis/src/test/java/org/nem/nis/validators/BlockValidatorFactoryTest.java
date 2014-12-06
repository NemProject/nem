package org.nem.nis.validators;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.time.TimeProvider;
import org.nem.nis.poi.PoiFacade;

import java.util.*;

public class BlockValidatorFactoryTest {

	@Test
	public void createReturnsValidValidator() {
		// Arrange:
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class));

		// Act:
		final BlockValidator validator = factory.create(Mockito.mock(PoiFacade.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	// TODO 20141201 J-B: nice tests; you might want to add something similar to the transaction factory tests
	// TODO 20141204 BR -> J: the TransactionValidatorFactory doesn't expose the builder to the public. You have an idea how to do it?
	// TODO 20141205: J-B: i guess we would need to do something similar; also, we might want to consider having a single test
	// > that validates against all types, i'm not sure if there's much value in splitting up the tests

	@Test
	public void createAddsTransactionDeadlineBlockValidator() {
		assertCreateAddsValidator(TransactionDeadlineBlockValidator.class);
	}

	@Test
	public void createAddsNonFutureEntityValidator() {
		assertCreateAddsValidator(NonFutureEntityValidator.class);
	}

	@Test
	public void createAddsEligibleSignerBlockValidator() {
		assertCreateAddsValidator(EligibleSignerBlockValidator.class);
	}

	@Test
	public void createAddsMaxTransactionsBlockValidator() {
		assertCreateAddsValidator(MaxTransactionsBlockValidator.class);
	}

	@Test
	public void createAddsNoSelfSignedTransactionsBlockValidator() {
		assertCreateAddsValidator(NoSelfSignedTransactionsBlockValidator.class);
	}

	@Test
	public void createAddsBlockImportanceTransferValidator() {
		assertCreateAddsValidator(BlockImportanceTransferValidator.class);
	}

	@Test
	public void createAddsBlockImportanceTransferBalanceValidator() {
		assertCreateAddsValidator(BlockImportanceTransferBalanceValidator.class);
	}

	private static void assertCreateAddsValidator(final Class<?> desiredClass) {
		// Arrange:
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class));

		// Act:
		final List<BlockValidator> validators = new ArrayList<>();
		factory.visitSubValidators(validators::add, Mockito.mock(PoiFacade.class));

		// Assert:
		Assert.assertThat(validators.size(), IsEqual.equalTo(7));
		Assert.assertThat(listContainsClass(validators, desiredClass), IsEqual.equalTo(true));
	}

	private static boolean listContainsClass(final List<BlockValidator> validators, final Class<?> desiredClass) {
		return validators.stream().anyMatch(v -> v.getClass().equals(desiredClass));
	}
}
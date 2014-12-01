package org.nem.nis.validators;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.poi.PoiFacade;

import java.util.List;

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

	@Test
	public void createAddsTransactionDeadlineBlockValidator() {
		assertCreateAddsValidator(new TransactionDeadlineBlockValidator());
	}

	@Test
	public void createAddsNonFutureEntityValidator() {
		assertCreateAddsValidator(new NonFutureEntityValidator(Mockito.mock(TimeProvider.class)));
	}

	@Test
	public void createAddsEligibleSignerBlockValidator() {
		assertCreateAddsValidator(new EligibleSignerBlockValidator(Mockito.mock(PoiFacade.class)));
	}

	@Test
	public void createAddsMaxTransactionsBlockValidator() {
		assertCreateAddsValidator(new MaxTransactionsBlockValidator());
	}

	@Test
	public void createAddsNoSelfSignedTransactionsBlockValidator() {
		assertCreateAddsValidator(new NoSelfSignedTransactionsBlockValidator(Mockito.mock(PoiFacade.class)));
	}

	@Test
	public void createAddsBlockImportanceTransferValidator() {
		assertCreateAddsValidator(new BlockImportanceTransferValidator());
	}

	@Test
	public void createAddsBlockImportanceTransferBalanceValidator() {
		assertCreateAddsValidator(new BlockImportanceTransferBalanceValidator());
	}

	private void assertCreateAddsValidator(final BlockValidator validator) {
		// Arrange:
		final ArgumentCaptor<BlockValidator> captor = ArgumentCaptor.forClass(BlockValidator.class);
		final AggregateBlockValidatorBuilder builder = Mockito.mock(AggregateBlockValidatorBuilder.class);
		final BlockValidatorFactory factory = new BlockValidatorFactory(Mockito.mock(TimeProvider.class));

		// Act:
		factory.create(builder, Mockito.mock(PoiFacade.class));

		// Assert:
		Mockito.verify(builder, Mockito.times(7)).add(captor.capture());
		Assert.assertThat(listContainsClass(captor.getAllValues(), validator.getClass()), IsEqual.equalTo(true));
	}

	private boolean listContainsClass(final List<BlockValidator> validators, Class<?> desiredClass) {
		return validators.stream()
				.map(v -> v.getClass().equals(desiredClass))
				.reduce((b1, b2) -> b1 || b2)
				.get();
	}
}
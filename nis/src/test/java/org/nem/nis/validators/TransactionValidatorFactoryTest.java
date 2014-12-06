package org.nem.nis.validators;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.HashCache;
import org.nem.core.time.TimeProvider;
import org.nem.nis.poi.*;

public class TransactionValidatorFactoryTest {

	@Test
	public void createReturnsValidValidator() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final SingleTransactionValidator validator = factory.create(Mockito.mock(PoiFacade.class), Mockito.mock(HashCache.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	@Test
	public void createSingleReturnsValidValidator() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final SingleTransactionValidator validator = factory.createSingle(Mockito.mock(PoiFacade.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	@Test
	public void createBatchReturnsValidValidator() {
		// Arrange:
		final TransactionValidatorFactory factory = createFactory();

		// Act:
		final BatchTransactionValidator validator = factory.createBatch(Mockito.mock(HashCache.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}

	private static TransactionValidatorFactory createFactory() {
		return new TransactionValidatorFactory(
				Mockito.mock(TimeProvider.class),
				Mockito.mock(PoiOptions.class));
	}
}
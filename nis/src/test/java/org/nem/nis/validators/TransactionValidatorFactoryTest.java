package org.nem.nis.validators;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.time.TimeProvider;
import org.nem.nis.dao.*;
import org.nem.nis.poi.*;

public class TransactionValidatorFactoryTest {

	@Test
	public void createReturnsValidValidator() {
		// Arrange:
		final TransactionValidatorFactory factory = new TransactionValidatorFactory(
				Mockito.mock(TransferDao.class),
				Mockito.mock(ImportanceTransferDao.class),
				Mockito.mock(TimeProvider.class),
				Mockito.mock(PoiOptions.class));

		// Act:
		final TransactionValidator validator = factory.create(Mockito.mock(PoiFacade.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}
}
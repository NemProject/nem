package org.nem.nis.validators;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.poi.PoiFacade;

public class TransactionValidatorFactoryTest {

	@Test
	public void createReturnsValidValidator() {
		// Arrange:
		final TransactionValidatorFactory factory = new TransactionValidatorFactory(Mockito.mock(TransferDao.class));

		// Act:
		final TransactionValidator validator = factory.create(Mockito.mock(PoiFacade.class));

		// Assert:
		Assert.assertThat(validator, IsNull.notNullValue());
	}
}
package org.nem.nis.validators.transaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class TSingleTransactionValidatorTest {

	@Test
	public void defaultGetNameReturnsTypeName() {
		// Arrange:
		final TSingleTransactionValidator<?> validator = new TransferTransactionValidator();

		// Act:
		final String name = validator.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("TransferTransactionValidator"));
	}
}

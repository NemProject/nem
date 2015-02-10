package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class SingleTransactionValidatorTest {

	@Test
	public void defaultGetNameReturnsTypeName() {
		// Arrange:
		final SingleTransactionValidator validator = new UniversalTransactionValidator();

		// Act:
		final String name = validator.getName();

		// Assert:
		Assert.assertThat(name, IsEqual.equalTo("UniversalTransactionValidator"));
	}
}
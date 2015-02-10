package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class BlockValidatorTest {

	@Test
	public void defaultGetNameReturnsTypeName() {
		// Arrange:
		final BlockValidator validator = new MaxTransactionsBlockValidator();

		// Act:
		final String name = validator.getName();

		// Assert:
		Assert.assertThat(name, IsEqual.equalTo("MaxTransactionsBlockValidator"));
	}
}
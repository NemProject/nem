package org.nem.nis.validators;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class NamedValidatorTest {

	@Test
	public void defaultGetNameReturnsTypeName() {
		// Arrange:
		final NamedValidator validator = new CrazyNameValidator();

		// Act:
		final String name = validator.getName();

		// Assert:
		MatcherAssert.assertThat(name, IsEqual.equalTo("CrazyNameValidator"));
	}

	private static class CrazyNameValidator implements NamedValidator {
	}
}

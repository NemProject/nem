package org.nem.nis.controller;

import org.junit.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.controller.interceptors.*;
import org.springframework.web.bind.WebDataBinder;

import java.util.stream.Collectors;

public class BinderControllerAdviceTest {

	@Test
	public void addBindersAddsAppropriateValidators() {
		// Arrange:
		final BinderControllerAdvice advice = new BinderControllerAdvice(null, null, null);
		final WebDataBinder binder = new WebDataBinder(null);

		// Act:
		advice.addBinders(binder, null);

		// Assert:
		final Class<?>[] expectedValidatorClasses = new Class<?>[] {
				InsecurePrivateKeyValidator.class,
				ConfiguredPrivateKeyValidator.class
		};
		Assert.assertThat(
				binder.getValidators().stream().map(v -> v.getClass()).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(expectedValidatorClasses));
	}
}
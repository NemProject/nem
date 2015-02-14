package org.nem.nis.controller;

import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.IsEquivalent;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.interceptors.*;
import org.springframework.web.bind.WebDataBinder;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

public class BinderControllerAdviceTest {

	@Test
	public void addBindersAddsAppropriateValidators() {
		// Arrange:
		final BinderControllerAdvice advice = new BinderControllerAdvice(
				Mockito.mock(LocalHostDetector.class),
				Mockito.mock(ReadOnlyAccountStateCache.class),
				Mockito.mock(NisConfiguration.class));
		final WebDataBinder binder = new WebDataBinder(null);

		// Act:
		advice.addBinders(binder, Mockito.mock(HttpServletRequest.class));

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
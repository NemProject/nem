package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.interceptors.*;
import org.nem.specific.deploy.NisConfiguration;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.stream.Collectors;

public class BinderControllerAdviceTest {

	@Test
	public void addBindersAddsPrivateKeyValidatorsForPrivateKeyTargets() {
		// Act:
		final Collection<Class<?>> validatorClasses = getValidatorClassesForTarget(new KeyPair().getPrivateKey());

		// Assert:
		final Class<?>[] expectedValidatorClasses = new Class<?>[]{
				InsecurePrivateKeyValidator.class, ConfiguredPrivateKeyValidator.class
		};
		MatcherAssert.assertThat(validatorClasses, IsEquivalent.equivalentTo(expectedValidatorClasses));
	}

	@Test
	public void addBindersAddsNoValidatorsForOtherTargets() {
		// Act:
		final Collection<Class<?>> validatorClasses = getValidatorClassesForTarget(new KeyPair().getPublicKey());

		// Assert:
		final Class<?>[] expectedValidatorClasses = new Class<?>[]{};
		MatcherAssert.assertThat(validatorClasses, IsEquivalent.equivalentTo(expectedValidatorClasses));
	}

	private static Collection<Class<?>> getValidatorClassesForTarget(final Object target) {
		// Arrange:
		final BinderControllerAdvice advice = new BinderControllerAdvice(Mockito.mock(LocalHostDetector.class),
				Mockito.mock(ReadOnlyAccountStateCache.class), Mockito.mock(NisConfiguration.class));
		final WebDataBinder binder = new WebDataBinder(target);

		// Act:
		advice.addBinders(binder, Mockito.mock(HttpServletRequest.class));
		return binder.getValidators().stream().map(Validator::getClass).collect(Collectors.toList());
	}
}

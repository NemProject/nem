package org.nem.nis.controller;

import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.interceptors.*;
import org.nem.specific.deploy.NisConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * ControllerAdvice-annotated class that initializes request parameter validators.
 */
@ControllerAdvice
public class BinderControllerAdvice {
	private final LocalHostDetector localHostDetector;
	private final ReadOnlyAccountStateCache accountStateCache;
	private final NisConfiguration nisConfiguration;

	/**
	 * Creates a new binder controller advice.
	 *
	 * @param localHostDetector The local host detector.
	 * @param accountStateCache The account state cache.
	 * @param nisConfiguration The NIS configuration.
	 */
	@Autowired(required = true)
	public BinderControllerAdvice(final LocalHostDetector localHostDetector, final ReadOnlyAccountStateCache accountStateCache,
			final NisConfiguration nisConfiguration) {
		this.localHostDetector = localHostDetector;
		this.accountStateCache = accountStateCache;
		this.nisConfiguration = nisConfiguration;
	}

	@InitBinder
	public void addBinders(final WebDataBinder binder, final HttpServletRequest request) {
		final Validator[] validators = new Validator[]{
				new InsecurePrivateKeyValidator(this.localHostDetector, this.accountStateCache, request),
				new ConfiguredPrivateKeyValidator(this.nisConfiguration.getAllowedHarvesterAddresses())
		};

		final Validator[] filteredValidators = Arrays.stream(validators)
				.filter(validator -> validator.supports(binder.getTarget().getClass())).toArray(Validator[]::new);

		binder.addValidators(filteredValidators);
	}
}

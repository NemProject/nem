package org.nem.nis.controller;

import org.nem.deploy.NisConfiguration;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.controller.interceptors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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
	 */
	@Autowired(required = true)
	public BinderControllerAdvice(
			final LocalHostDetector localHostDetector,
			final ReadOnlyAccountStateCache accountStateCache,
			final NisConfiguration nisConfiguration) {
		this.localHostDetector = localHostDetector;
		this.accountStateCache = accountStateCache;
		this.nisConfiguration = nisConfiguration;
	}

	@InitBinder
	public void addBinders(final WebDataBinder binder, final HttpServletRequest request) {
		binder.addValidators(new InsecurePrivateKeyValidator(this.localHostDetector, this.accountStateCache, request));
		binder.addValidators(new ConfiguredPrivateKeyValidator(this.nisConfiguration.getAllowedHarvesterAddresses()));
	}
}
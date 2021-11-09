package org.nem.nis.controller.interceptors;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.validation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * A spring validator used to lock down private key request arguments such that remote requests with non-remote harvester private keys are
 * blocked
 */
public class InsecurePrivateKeyValidator implements Validator {
	private static final Logger LOGGER = Logger.getLogger(InsecurePrivateKeyValidator.class.getName());

	private final LocalHostDetector localHostDetector;
	private final ReadOnlyAccountStateCache accountStateCache;
	private final HttpServletRequest request;

	/**
	 * Creates a new validator.
	 *
	 * @param localHostDetector The local host detector.
	 * @param accountStateCache The readonly account state cache.
	 * @param request The request.
	 */
	public InsecurePrivateKeyValidator(final LocalHostDetector localHostDetector, final ReadOnlyAccountStateCache accountStateCache,
			final HttpServletRequest request) {
		this.localHostDetector = localHostDetector;
		this.request = request;
		this.accountStateCache = accountStateCache;
	}

	@Override
	public boolean supports(final Class<?> clazz) {
		return PrivateKey.class == clazz;
	}

	@Override
	public void validate(final Object target, final Errors errors) {
		if (this.localHostDetector.isLocal(this.request)) {
			return;
		}

		final PrivateKey privateKey = (PrivateKey) target;
		final Address address = Address.fromPublicKey(new KeyPair(privateKey).getPublicKey());
		final ReadOnlyAccountState accountState = this.accountStateCache.findStateByAddress(address);
		if (!accountState.getRemoteLinks().isRemoteHarvester()) {
			final String message = String.format("remote %s attempted to call local %s with non-remote address %s",
					this.request.getRemoteAddr(), this.request.getRequestURI(), address);
			LOGGER.warning(message);
			throw new UnauthorizedAccessException(message);
		}
	}
}

package org.nem.nis.controller.interceptors;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.springframework.validation.*;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A spring validator used to lock down private key request arguments such that requests with non-configured harvester private keys are
 * blocked
 */
public class ConfiguredPrivateKeyValidator implements Validator {
	private static final Logger LOGGER = Logger.getLogger(ConfiguredPrivateKeyValidator.class.getName());

	private final Address[] allowedAddresses;

	/**
	 * Creates a new validator.
	 *
	 * @param allowedAddresses The addresses that are allowed for private key requests.
	 */
	public ConfiguredPrivateKeyValidator(final Address[] allowedAddresses) {
		this.allowedAddresses = allowedAddresses;
	}

	@Override
	public boolean supports(final Class<?> clazz) {
		return PrivateKey.class == clazz;
	}

	@Override
	public void validate(final Object target, final Errors errors) {
		final PrivateKey privateKey = (PrivateKey) target;
		final Address address = Address.fromPublicKey(new KeyPair(privateKey).getPublicKey());

		final boolean isAddressAllowed = Arrays.stream(this.allowedAddresses).anyMatch(allowedAddress -> allowedAddress.equals(address));

		if (this.allowedAddresses.length > 0 && !isAddressAllowed) {
			final String message = String.format("blocking private key request with non-configured address %s", address);
			LOGGER.warning(message);
			throw new UnauthorizedAccessException(message);
		}
	}
}

package org.nem.nis.validators;

import org.nem.core.model.*;

/**
 * Base class for validators that validates entities match the default network
 */
public abstract class NetworkValidator {

	/**
	 * Validates the specified verifiable entity.
	 *
	 * @param entity The entity.
	 * @return The validation result.
	 */
	protected ValidationResult validateNetwork(final VerifiableEntity entity) {
		final byte version = (byte) ((entity.getVersion() & 0xFF000000) >> 24);
		return version == NetworkInfos.getDefault().getVersion() ? ValidationResult.SUCCESS : ValidationResult.FAILURE_WRONG_NETWORK;
	}
}

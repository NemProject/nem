package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

/**
 * Block validator that validates entities match the default network
 */
public class BlockNetworkValidator extends NetworkValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		return this.validateNetwork(block);
	}
}

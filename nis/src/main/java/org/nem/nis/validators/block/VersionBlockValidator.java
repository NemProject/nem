package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.validators.BlockValidator;

/**
 * A BlockValidator implementation that validates that higher versioned blocks do not appear before the respective fork heights
 */
public class VersionBlockValidator implements BlockValidator {

	@Override
	public ValidationResult validate(final Block block) {
		return 1 == block.getEntityVersion() ? ValidationResult.SUCCESS : ValidationResult.FAILURE_ENTITY_INVALID_VERSION;
	}
}

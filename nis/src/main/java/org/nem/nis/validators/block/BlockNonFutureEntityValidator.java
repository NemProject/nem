package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.core.time.TimeProvider;
import org.nem.nis.validators.*;

/**
 * Block validator that validates the block time stamp is not too far in the future.
 */
public class BlockNonFutureEntityValidator extends NonFutureEntityValidator implements BlockValidator {

	/**
	 * Creates a new validator.
	 *
	 * @param timeProvider The time provider.
	 */
	public BlockNonFutureEntityValidator(final TimeProvider timeProvider) {
		super(timeProvider);
	}

	@Override
	public ValidationResult validate(final Block block) {
		return this.validateTimeStamp(block.getTimeStamp());
	}
}

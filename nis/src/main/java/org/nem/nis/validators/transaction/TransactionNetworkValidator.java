package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

/**
 * Single transaction validator that validates entities match the default network
 */
public class TransactionNetworkValidator extends NetworkValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return this.validateNetwork(transaction);
	}
}

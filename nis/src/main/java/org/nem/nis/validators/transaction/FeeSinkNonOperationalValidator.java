package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicConstants;
import org.nem.nis.validators.ValidationContext;
import org.nem.nis.ForkConfiguration;

import java.util.logging.Logger;
import java.util.Arrays;

/**
 * A transaction validator that validates that none of the fee sink accounts can initiate a transaction.
 */
public class FeeSinkNonOperationalValidator implements TSingleTransactionValidator<MultisigTransaction> {
	private static final Logger LOGGER = Logger.getLogger(FeeSinkNonOperationalValidator.class.getName());
	private static final Address[] SINK_ADDRESSES = new Address[]{
			MosaicConstants.MOSAIC_CREATION_FEE_SINK.getAddress(), MosaicConstants.NAMESPACE_OWNER_NEM.getAddress()
	};

	private final ForkConfiguration forkConfiguration;

	/**
	 * Creates a validator.
	 *
	 * @param forkConfiguration The fork configuration.
	 */
	public FeeSinkNonOperationalValidator(final ForkConfiguration forkConfiguration) {
		this.forkConfiguration = forkConfiguration;
	}

	@Override
	public ValidationResult validate(final MultisigTransaction transaction, final ValidationContext context) {
		if (context.getBlockHeight().compareTo(this.forkConfiguration.getTreasuryReissuanceForkHeight()) <= 0) {
			return ValidationResult.SUCCESS;
		}

		final Address initiatingAddress = transaction.getOtherTransaction().getSigner().getAddress();
		if (!Arrays.stream(SINK_ADDRESSES).anyMatch(address -> initiatingAddress.equals(address))) {
			return ValidationResult.SUCCESS;
		}

		LOGGER.warning(String.format("Transaction initiated by %s rejected at height %s", initiatingAddress, context.getBlockHeight()));
		return ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG;
	}
}

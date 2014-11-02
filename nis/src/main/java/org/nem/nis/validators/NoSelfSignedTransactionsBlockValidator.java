package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.poi.*;

/**
 * Validator that checks the block signer and rejects a block if any transactions are self-signed.
 */
public class NoSelfSignedTransactionsBlockValidator implements BlockValidator {
	private final PoiFacade poiFacade;

	/**
	 * Creates a validator.
	 *
	 * @param poiFacade The poi facade.
	 */
	public NoSelfSignedTransactionsBlockValidator(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final Address harvesterAddress = block.getSigner().getAddress();
		final PoiAccountState ownerState = this.poiFacade.findForwardedStateByAddress(harvesterAddress, block.getHeight());
		final boolean isSelfSigned = block.getTransactions().stream().anyMatch(transaction -> {
			final Address signerAddress = transaction.getSigner().getAddress();
			return signerAddress.equals(harvesterAddress) || signerAddress.equals(ownerState.getAddress());
		});

		return isSelfSigned
				? ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION
				: ValidationResult.SUCCESS;
	}
}

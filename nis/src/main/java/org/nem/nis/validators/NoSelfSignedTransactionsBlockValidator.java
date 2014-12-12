package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;

/**
 * Validator that checks the block signer and rejects a block if any transactions are self-signed.
 */
public class NoSelfSignedTransactionsBlockValidator implements BlockValidator {
	private final AccountStateRepository accountStateRepository;

	/**
	 * Creates a validator.
	 *
	 * @param accountStateRepository The poi facade.
	 */
	public NoSelfSignedTransactionsBlockValidator(final AccountStateRepository accountStateRepository) {
		this.accountStateRepository = accountStateRepository;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final Address harvesterAddress = block.getSigner().getAddress();
		final AccountState ownerState = this.accountStateRepository.findForwardedStateByAddress(harvesterAddress, block.getHeight());
		final boolean isSelfSigned = block.getTransactions().stream().anyMatch(transaction -> {
			final Address signerAddress = transaction.getSigner().getAddress();
			return signerAddress.equals(harvesterAddress) || signerAddress.equals(ownerState.getAddress());
		});

		return isSelfSigned
				? ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION
				: ValidationResult.SUCCESS;
	}
}

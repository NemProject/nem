package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.validators.BlockValidator;

/**
 * Validator that checks the block signer and rejects a block if any transactions are self-signed.
 */
public class NoSelfSignedTransactionsBlockValidator implements BlockValidator {
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a validator.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public NoSelfSignedTransactionsBlockValidator(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public ValidationResult validate(final Block block) {
		final Address harvesterAddress = block.getSigner().getAddress();
		final ReadOnlyAccountState ownerState = this.accountStateCache.findForwardedStateByAddress(harvesterAddress, block.getHeight());
		final boolean isSelfSigned = block.getTransactions().stream().anyMatch(transaction -> {
			final Address signerAddress = transaction.getSigner().getAddress();
			return signerAddress.equals(harvesterAddress) || signerAddress.equals(ownerState.getAddress());
		});

		return isSelfSigned ? ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION : ValidationResult.SUCCESS;
	}
}

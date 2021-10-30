package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

/**
 * A simple predicate class that is used to check if a multisig signature matches a multisig transaction.
 */
public class MultisigSignatureMatchPredicate {
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a predicate.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public MultisigSignatureMatchPredicate(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	/**
	 * Determines if the multisig signature matches the multisig transaction.
	 *
	 * @param signatureTransaction The signature transaction.
	 * @param multisigTransaction The multisig transaction.
	 * @return true if the multisig signature matches the multisig transaction.
	 */
	public boolean isMatch(final MultisigSignatureTransaction signatureTransaction, final MultisigTransaction multisigTransaction) {
		final ReadOnlyAccountState cosignerState = this.accountStateCache.findStateByAddress(signatureTransaction.getSigner().getAddress());
		final Address multisigAddress = multisigTransaction.getOtherTransaction().getSigner().getAddress();
		return !signatureTransaction.getSigner().equals(multisigTransaction.getSigner())
				&& signatureTransaction.getOtherTransactionHash().equals(multisigTransaction.getOtherTransactionHash())
				&& signatureTransaction.getDebtor().equals(multisigTransaction.getOtherTransaction().getSigner())
				&& cosignerState.getMultisigLinks().isCosignatoryOf(multisigAddress);
	}
}

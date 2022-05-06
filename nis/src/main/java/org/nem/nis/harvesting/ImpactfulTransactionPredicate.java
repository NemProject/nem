package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.function.BiPredicate;

/**
 * A bi-predicate that tests whether or not a transaction impacts an account.
 */
public class ImpactfulTransactionPredicate implements BiPredicate<Address, Transaction> {
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new predicate.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public ImpactfulTransactionPredicate(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public boolean test(final Address address, final Transaction transaction) {
		return matchAddress(transaction, address) || this.isCosignatory(transaction, address);
	}

	private static boolean matchAddress(final Transaction transaction, final Address address) {
		return transaction.getAccounts().stream().map(Account::getAddress)
				.anyMatch(transactionAddress -> transactionAddress.equals(address));
	}

	private boolean isCosignatory(final Transaction transaction, final Address address) {
		if (TransactionTypes.MULTISIG != transaction.getType()) {
			return false;
		}

		final ReadOnlyAccountState state = this.accountStateCache.findStateByAddress(address);
		return state.getMultisigLinks().isCosignatoryOf(((MultisigTransaction) transaction).getOtherTransaction().getSigner().getAddress());
	}
}

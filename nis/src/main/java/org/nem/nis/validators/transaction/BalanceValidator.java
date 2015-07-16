package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.validators.*;

import java.util.*;

/**
 * A validator that checks whether or not all debited accounts have sufficient balance.
 */
public class BalanceValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * TODO 20150716 BR -> J: i think the way things are handled in this class is a relict from times where we were executing transactions block wise.
	 * > Since we a executing transactions right after validating them now, it should be safe to inject and use the read only cache.
	 *
	 * Creates a new balance validator.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public BalanceValidator(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final NegativeBalanceCheckTransferObserver observer = new NegativeBalanceCheckTransferObserver(context.getDebitPredicate(), this.accountStateCache);
		transaction.execute(new TransferObserverToTransactionObserverAdapter(observer));
		return observer.hasNegativeBalances() ? ValidationResult.FAILURE_INSUFFICIENT_BALANCE : ValidationResult.SUCCESS;
	}

	private static class NegativeBalanceCheckTransferObserver implements TransferObserver {
		private final DebitPredicate debitPredicate;
		private final Map<Account, Long> accountToBalanceMap = new HashMap<>();
		private final ReadOnlyAccountStateCache accountStateCache;
		private boolean hasNegativeBalances;

		public NegativeBalanceCheckTransferObserver(final DebitPredicate debitPredicate, final ReadOnlyAccountStateCache accountStateCache) {
			this.debitPredicate = debitPredicate;
			this.accountStateCache = accountStateCache;
		}

		public boolean hasNegativeBalances() {
			return this.hasNegativeBalances;
		}

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final SmartTile smartTile) {
			this.notifyDebit(sender, smartTile);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			this.adjustBalance(account, amount.getNumMicroNem());
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			this.adjustBalance(account, -amount.getNumMicroNem());
		}

		private void adjustBalance(final Account account, final Long delta) {
			Long balance = this.accountToBalanceMap.getOrDefault(account, 0L);
			balance += delta;

			if (balance < 0) {
				// could optimize this, to only call once for account, but this is not likely to be a bottleneck
				this.hasNegativeBalances = this.hasNegativeBalances || !this.debitPredicate.canDebit(account, Amount.fromMicroNem(-1 * balance));
			}

			this.accountToBalanceMap.put(account, balance);
		}

		public void notifyDebit(final Account account, final SmartTile smartTile) {
			final ReadOnlyAccountState state = this.accountStateCache.findStateByAddress(account.getAddress());
			final SmartTile accountSmartTile = state.getSmartTileMap().get(smartTile.getMosaicId());
			this.hasNegativeBalances = accountSmartTile.getQuantity().compareTo(smartTile.getQuantity()) < 0;
		}
	}
}

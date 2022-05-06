package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.validators.*;

import java.util.*;

/**
 * A validator that checks whether or not all debited accounts have sufficient mosaics.
 */
public class MosaicBalanceValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final NegativeMosaicBalanceCheckTransactionObserver observer = new NegativeMosaicBalanceCheckTransactionObserver(
				context.getState()::canDebit);
		transaction.execute(observer, context.getState().transactionExecutionState());
		return observer.hasNegativeBalances() ? ValidationResult.FAILURE_INSUFFICIENT_BALANCE : ValidationResult.SUCCESS;
	}

	private static class NegativeMosaicBalanceCheckTransactionObserver implements TransactionObserver {
		private final AccountToMosaicsMap accountToMosaicsMap;

		public NegativeMosaicBalanceCheckTransactionObserver(final DebitPredicate<Mosaic> debitPredicate) {
			this.accountToMosaicsMap = new AccountToMosaicsMap(debitPredicate);
		}

		public boolean hasNegativeBalances() {
			return this.accountToMosaicsMap.hasNegativeBalances();
		}

		@Override
		public void notify(final Notification notification) {
			if (NotificationType.MosaicTransfer != notification.getType()) {
				return;
			}

			final MosaicTransferNotification n = (MosaicTransferNotification) notification;
			this.notifyDebit(n.getSender(), n.getMosaicId(), n.getQuantity());
			this.notifyCredit(n.getRecipient(), n.getMosaicId(), n.getQuantity());
		}

		private void notifyCredit(final Account account, final MosaicId mosaicId, final Quantity amount) {
			this.accountToMosaicsMap.adjustMosaicBalance(account, mosaicId, amount.getRaw());
		}

		private void notifyDebit(final Account account, final MosaicId mosaicId, final Quantity amount) {
			this.accountToMosaicsMap.adjustMosaicBalance(account, mosaicId, -amount.getRaw());
		}

		private static class AccountToMosaicsMap {
			private final DebitPredicate<Mosaic> debitPredicate;
			private final Map<Account, Map<MosaicId, Long>> map = new HashMap<>();
			private boolean hasNegativeBalances;

			public AccountToMosaicsMap(final DebitPredicate<Mosaic> debitPredicate) {
				this.debitPredicate = debitPredicate;
			}

			public boolean hasNegativeBalances() {
				return this.hasNegativeBalances;
			}

			public void adjustMosaicBalance(final Account account, final MosaicId mosaicId, final Long delta) {
				final Map<MosaicId, Long> mosaics = this.map.getOrDefault(account, new HashMap<>());
				this.map.put(account, mosaics);

				Long balance = mosaics.getOrDefault(mosaicId, 0L);
				balance += delta;
				mosaics.put(mosaicId, balance);

				if (balance < 0) {
					final Mosaic mosaic = new Mosaic(mosaicId, Quantity.fromValue(-1 * balance));
					this.hasNegativeBalances = this.hasNegativeBalances || !this.debitPredicate.canDebit(account, mosaic);
				}
			}
		}
	}
}

package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.validators.*;

import java.util.*;

/**
 * A validator that checks whether or not all debited accounts have sufficient balance.
 */
public class BalanceValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final NegativeBalanceCheckTransferObserver observer = new NegativeBalanceCheckTransferObserver(context.getState()::canDebit);
		transaction.execute(observer, context.getState().transactionExecutionState());
		return observer.hasNegativeBalances() ? ValidationResult.FAILURE_INSUFFICIENT_BALANCE : ValidationResult.SUCCESS;
	}

	private static class NegativeBalanceCheckTransferObserver implements TransactionObserver {
		private final DebitPredicate<Amount> debitPredicate;
		private final Map<Account, Long> accountToBalanceMap = new HashMap<>();
		private boolean hasNegativeBalances;

		public NegativeBalanceCheckTransferObserver(final DebitPredicate<Amount> debitPredicate) {
			this.debitPredicate = debitPredicate;
		}

		public boolean hasNegativeBalances() {
			return this.hasNegativeBalances;
		}

		@Override
		public void notify(final Notification notification) {
			switch (notification.getType()) {
				case BalanceTransfer:
					this.notify((BalanceTransferNotification) notification);
					break;

				case BalanceCredit:
				case BalanceDebit:
					this.notify((BalanceAdjustmentNotification) notification);
					break;
				default :
					break;
			}
		}

		private void notify(final BalanceTransferNotification notification) {
			this.notifyDebit(notification.getSender(), notification.getAmount());
			this.notifyCredit(notification.getRecipient(), notification.getAmount());
		}

		private void notify(final BalanceAdjustmentNotification notification) {
			if (NotificationType.BalanceCredit == notification.getType()) {
				this.notifyCredit(notification.getAccount(), notification.getAmount());
			} else {
				this.notifyDebit(notification.getAccount(), notification.getAmount());
			}
		}

		private void notifyCredit(final Account account, final Amount amount) {
			this.adjustBalance(account, amount.getNumMicroNem());
		}

		private void notifyDebit(final Account account, final Amount amount) {
			this.adjustBalance(account, -amount.getNumMicroNem());
		}

		private void adjustBalance(final Account account, final Long delta) {
			Long balance = this.accountToBalanceMap.getOrDefault(account, 0L);
			balance += delta;

			if (balance < 0) {
				this.hasNegativeBalances = this.hasNegativeBalances
						|| !this.debitPredicate.canDebit(account, Amount.fromMicroNem(-1 * balance));
			}

			this.accountToBalanceMap.put(account, balance);
		}
	}
}

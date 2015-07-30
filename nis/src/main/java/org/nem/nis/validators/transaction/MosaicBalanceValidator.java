package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.secret.*;
import org.nem.nis.validators.*;

import java.util.*;

/**
 * A validator that checks whether or not all debited accounts have sufficient mosaics.
 */
public class MosaicBalanceValidator implements SingleTransactionValidator {

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		final NegativeMosaicBalanceCheckTransferObserver observer = new NegativeMosaicBalanceCheckTransferObserver(context.getMosaicDebitPredicate());
		final BlockNotificationContext bnContext = new BlockNotificationContext(BlockHeight.MAX, TimeInstant.ZERO, NotificationTrigger.Execute);
		transaction.execute(new BlockTransactionObserverToTransactionObserverAdapter(observer, bnContext));
		return observer.hasNegativeBalances() ? ValidationResult.FAILURE_INSUFFICIENT_BALANCE : ValidationResult.SUCCESS;
	}

	private static class NegativeMosaicBalanceCheckTransferObserver implements BlockTransactionObserver {
		private final AccountToMosaicsMap accountToMosaicsMap;
		private boolean hasNegativeBalances;

		public NegativeMosaicBalanceCheckTransferObserver(final DebitPredicate<Mosaic> debitPredicate) {
			this.accountToMosaicsMap =  new AccountToMosaicsMap(debitPredicate);
		}

		public boolean hasNegativeBalances() {
			return this.hasNegativeBalances;
		}

		@Override
		public void notify(final Notification notification, final BlockNotificationContext context) {
			if (notification.getType() != NotificationType.MosaicTransfer) {
				return;
			}

			final MosaicTransferNotification n = (MosaicTransferNotification)notification;
			this.hasNegativeBalances |= this.accountToMosaicsMap.adjustMosaicBalance(n.getSender(), n.getMosaicId(), -n.getQuantity().getRaw());
			this.hasNegativeBalances |= this.accountToMosaicsMap.adjustMosaicBalance(n.getRecipient(), n.getMosaicId(), n.getQuantity().getRaw());
		}

		private class AccountToMosaicsMap {
			private final DebitPredicate<Mosaic> debitPredicate;
			private final Map<Account, Map<MosaicId, Long>> map = new HashMap<>();

			private AccountToMosaicsMap(final DebitPredicate<Mosaic> debitPredicate) {
				this.debitPredicate = debitPredicate;
			}

			private boolean adjustMosaicBalance(final Account account, final MosaicId mosaicId, final Long delta) {
				final Map<MosaicId, Long> mosaics = this.map.getOrDefault(account, new HashMap<>());
				this.map.put(account, mosaics);
				Long balance = mosaics.getOrDefault(mosaicId, 0L);
				balance += delta;
				mosaics.put(mosaicId, balance);
				return balance.compareTo(0L) < 0 && !this.debitPredicate.canDebit(account, new Mosaic(mosaicId, Quantity.fromValue(-1 * balance)));
			}
		}
	}
}

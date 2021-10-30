package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

/**
 * Observer that intercepts MinCosignatoriesModification to update the minimum number of cosignatories a multisig account requires.
 */
public class MultisigMinCosignatoriesModificationObserver implements BlockTransactionObserver {
	private final AccountStateCache stateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public MultisigMinCosignatoriesModificationObserver(final AccountStateCache accountStateCache) {
		this.stateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.MinCosignatoriesModification) {
			return;
		}

		this.notify((MultisigMinCosignatoriesModificationNotification) notification, context);
	}

	private void notify(final MultisigMinCosignatoriesModificationNotification notification, final BlockNotificationContext context) {
		final Address multisigAddress = notification.getMultisigAccount().getAddress();
		final AccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		final boolean execute = NotificationTrigger.Execute == context.getTrigger();
		final MultisigMinCosignatoriesModification modification = notification.getModification();
		multisigState.getMultisigLinks().incrementMinCosignatoriesBy(modification.getRelativeChange() * (execute ? 1 : -1));
	}
}

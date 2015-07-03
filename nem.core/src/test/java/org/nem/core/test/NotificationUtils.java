package org.nem.core.test;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;

import java.util.Collection;

/**
 * Static class providing helper functions for validating notifications.
 */
public class NotificationUtils {

	/**
	 * Asserts that the specified notification is an account notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedAccount The expected account.
	 */
	public static void assertAccountNotification(final Notification notification, final Account expectedAccount) {
		final AccountNotification n = (AccountNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.Account));
		Assert.assertThat(n.getAccount(), IsEqual.equalTo(expectedAccount));
	}

	/**
	 * Asserts that the specified notification is a balance credit notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedAccount The expected account.
	 * @param expectedAmount The expected amount.
	 */
	public static void assertBalanceCreditNotification(final Notification notification, final Account expectedAccount, final Amount expectedAmount) {
		final BalanceAdjustmentNotification n = (BalanceAdjustmentNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.BalanceCredit));
		Assert.assertThat(n.getAccount(), IsEqual.equalTo(expectedAccount));
		Assert.assertThat(n.getAmount(), IsEqual.equalTo(expectedAmount));
	}

	/**
	 * Asserts that the specified notification is a balance debit notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedAccount The expected account.
	 * @param expectedAmount The expected amount.
	 */
	public static void assertBalanceDebitNotification(final Notification notification, final Account expectedAccount, final Amount expectedAmount) {
		final BalanceAdjustmentNotification n = (BalanceAdjustmentNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.BalanceDebit));
		Assert.assertThat(n.getAccount(), IsEqual.equalTo(expectedAccount));
		Assert.assertThat(n.getAmount(), IsEqual.equalTo(expectedAmount));
	}

	/**
	 * Asserts that the specified notification is a block harvest notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedAccount The expected account.
	 * @param expectedAmount The expected amount.
	 */
	public static void assertBlockHarvestNotification(final Notification notification, final Account expectedAccount, final Amount expectedAmount) {
		final BalanceAdjustmentNotification n = (BalanceAdjustmentNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.BlockHarvest));
		Assert.assertThat(n.getAccount(), IsEqual.equalTo(expectedAccount));
		Assert.assertThat(n.getAmount(), IsEqual.equalTo(expectedAmount));
	}

	/**
	 * Asserts that the specified notification is a balance transfer notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedSender The expected sender.
	 * @param expectedRecipient The expected recipient.
	 * @param expectedAmount The expected amount.
	 */
	public static void assertBalanceTransferNotification(
			final Notification notification,
			final Account expectedSender,
			final Account expectedRecipient,
			final Amount expectedAmount) {
		final BalanceTransferNotification n = (BalanceTransferNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.BalanceTransfer));
		Assert.assertThat(n.getSender(), IsEqual.equalTo(expectedSender));
		Assert.assertThat(n.getRecipient(), IsEqual.equalTo(expectedRecipient));
		Assert.assertThat(n.getAmount(), IsEqual.equalTo(expectedAmount));
	}

	/**
	 * Asserts that the specified notification is a importance transfer notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedLessor The expected lessor.
	 * @param expectedLessee The expected lessee.
	 * @param expectedMode The expected mode.
	 */
	public static void assertImportanceTransferNotification(
			final Notification notification,
			final Account expectedLessor,
			final Account expectedLessee,
			final ImportanceTransferMode expectedMode) {
		final ImportanceTransferNotification n = (ImportanceTransferNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.ImportanceTransfer));
		Assert.assertThat(n.getLessor(), IsEqual.equalTo(expectedLessor));
		Assert.assertThat(n.getLessee(), IsEqual.equalTo(expectedLessee));
		Assert.assertThat(n.getMode(), IsEqual.equalTo(expectedMode));
	}

	/**
	 * Asserts that the specified notification is a transaction hashes notification.
	 *
	 * @param notification The notification to test.
	 * @param pairs The expected transaction hashes.
	 */
	public static void assertTransactionHashesNotification(final Notification notification, final Collection<HashMetaDataPair> pairs) {
		final TransactionHashesNotification n = (TransactionHashesNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.TransactionHashes));
		Assert.assertThat(n.getPairs(), IsEquivalent.equivalentTo(pairs));
	}

	/**
	 * Asserts that the specified notification is a cosignatory modification notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedMultisig The expected multisig account.
	 * @param expectedModification The expected multisig cosignatory modification.
	 */
	public static void assertCosignatoryModificationNotification(
			final Notification notification,
			final Account expectedMultisig,
			final MultisigCosignatoryModification expectedModification) {
		final MultisigCosignatoryModificationNotification n = (MultisigCosignatoryModificationNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.CosignatoryModification));
		Assert.assertThat(n.getMultisigAccount(), IsEqual.equalTo(expectedMultisig));
		Assert.assertThat(n.getModification(), IsEqual.equalTo(expectedModification));
	}

	/**
	 * Asserts that the specified notification is a minimum cosignatories modification notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedMultisig The expected multisig account.
	 * @param expectedModification The expected multisig minimum cosignatories modification.
	 */
	public static void assertMinCosignatoriesModificationNotification(
			final Notification notification,
			final Account expectedMultisig,
			final MultisigMinCosignatoriesModification expectedModification) {
		final MultisigMinCosignatoriesModificationNotification n = (MultisigMinCosignatoriesModificationNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.MinCosignatoriesModification));
		Assert.assertThat(n.getMultisigAccount(), IsEqual.equalTo(expectedMultisig));
		Assert.assertThat(n.getModification(), IsEqual.equalTo(expectedModification));
	}

	/**
	 * Asserts that the specified notification is a provision namespace notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedOwner The expected owner.
	 * @param expectedNamespaceId The expected namespace id.
	 */
	public static void assertProvisionNamespaceNotification(
			final Notification notification,
			final Account expectedOwner,
			final NamespaceId expectedNamespaceId) {
		final ProvisionNamespaceNotification n = (ProvisionNamespaceNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.ProvisionNamespace));
		Assert.assertThat(n.getOwner(), IsEqual.equalTo(expectedOwner));
		Assert.assertThat(n.getNamespaceId(), IsEqual.equalTo(expectedNamespaceId));
	}

	public static void assertMosaicCreationNotification(final Notification notification, final Mosaic mosaic) {
		final MosaicCreationNotification n = (MosaicCreationNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.MosaicCreation));

		Assert.assertThat(n.getMosaic().getCreator(), IsEqual.equalTo(mosaic.getCreator()));
		Assert.assertThat(n.getMosaic().getAmount(), IsEqual.equalTo(mosaic.getAmount()));

		// TODO 20150702: G-? how do we want to do this?
		//Assert.assertThat(n.getMosaic().getChildren(), IsEqual.equalTo(mosaic));
		Assert.assertThat(n.getMosaic().getName(), IsEqual.equalTo(mosaic.getName()));
		Assert.assertThat(n.getMosaic().getDescription(), IsEqual.equalTo(mosaic.getDescription()));
		Assert.assertThat(n.getMosaic().getDivisibility(), IsEqual.equalTo(mosaic.getDivisibility()));
		Assert.assertThat(n.getMosaic().getNamespaceId(), IsEqual.equalTo(mosaic.getNamespaceId()));
		Assert.assertThat(n.getMosaic().isQuantityMutable(), IsEqual.equalTo(mosaic.isQuantityMutable()));
		Assert.assertThat(n.getMosaic().isTransferable(), IsEqual.equalTo(mosaic.isTransferable()));
	}
}

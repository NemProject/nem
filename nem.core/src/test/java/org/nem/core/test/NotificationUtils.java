package org.nem.core.test;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;

import java.util.Collection;

/**
 * Static class providing helper functions for creating and validating notifications.
 */
public class NotificationUtils {

	//region notify *

	/**
	 * Raises a credit notification on the specified observer.
	 *
	 * @param observer The observer.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public static void notifyCredit(final TransactionObserver observer, final Account account, final Amount amount) {
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceCredit, account, amount));
	}

	/**
	 * Raises a debit notification on the specified observer.
	 *
	 * @param observer The observer.
	 * @param account The account.
	 * @param amount The amount.
	 */
	public static void notifyDebit(final TransactionObserver observer, final Account account, final Amount amount) {
		observer.notify(new BalanceAdjustmentNotification(NotificationType.BalanceDebit, account, amount));
	}

	/**
	 * Raises a balance transfer notification on the specified observer.
	 *
	 * @param observer The observer.
	 * @param sender The sender.
	 * @param recipient The recipient.
	 * @param amount The amount.
	 */
	public static void notifyTransfer(final TransactionObserver observer, final Account sender, final Account recipient, final Amount amount) {
		observer.notify(new BalanceTransferNotification(sender, recipient, amount));
	}

	//endregion

	//region assert *

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
	 * Asserts that the specified notification is a mosaic transfer notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedSender The expected sender.
	 * @param expectedRecipient The expected recipient.
	 * @param expectedMosaicId The expected mosaic id.
	 * @param expectedQuantity The expected quantity.
	 */
	public static void assertMosaicTransferNotification(
			final Notification notification,
			final Account expectedSender,
			final Account expectedRecipient,
			final MosaicId expectedMosaicId,
			final Quantity expectedQuantity) {
		final MosaicTransferNotification n = (MosaicTransferNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.MosaicTransfer));
		Assert.assertThat(n.getSender(), IsEqual.equalTo(expectedSender));
		Assert.assertThat(n.getRecipient(), IsEqual.equalTo(expectedRecipient));
		Assert.assertThat(n.getMosaicId(), IsEqual.equalTo(expectedMosaicId));
		Assert.assertThat(n.getQuantity(), IsEqual.equalTo(expectedQuantity));
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

	/**
	 * Asserts that the specified notification is a mosaic definition creation notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedMosaicDefinition The expected mosaic definition.
	 */
	public static void assertMosaicDefinitionCreationNotification(final Notification notification, final MosaicDefinition expectedMosaicDefinition) {
		final MosaicDefinitionCreationNotification n = (MosaicDefinitionCreationNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.MosaicDefinitionCreation));

		Assert.assertThat(n.getMosaicDefinition().getCreator(), IsEqual.equalTo(expectedMosaicDefinition.getCreator()));
		Assert.assertThat(n.getMosaicDefinition().getId(), IsEqual.equalTo(expectedMosaicDefinition.getId()));
		Assert.assertThat(n.getMosaicDefinition().getDescriptor(), IsEqual.equalTo(expectedMosaicDefinition.getDescriptor()));

		final MosaicProperties properties = n.getMosaicDefinition().getProperties();
		final MosaicProperties expectedProperties = expectedMosaicDefinition.getProperties();
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(expectedProperties.getDivisibility()));
		Assert.assertThat(properties.getInitialSupply(), IsEqual.equalTo(expectedProperties.getInitialSupply()));
		Assert.assertThat(properties.isSupplyMutable(), IsEqual.equalTo(expectedProperties.isSupplyMutable()));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(expectedProperties.isTransferable()));
	}

	/**
	 * Asserts that the specified notification is a mosaic supply change notification.
	 *
	 * @param notification The notification to test.
	 * @param expectedSupplier The expected supplier.
	 * @param expectedMosaicId The expected mosaic id.
	 * @param expectedSupplyChange The expected supply change.
	 * @param expectedSupplyType The expected supply type.
	 */
	public static void assertMosaicSupplyChangeNotification(
			final Notification notification,
			final Account expectedSupplier,
			final MosaicId expectedMosaicId,
			final Supply expectedSupplyChange,
			final MosaicSupplyType expectedSupplyType) {
		final MosaicSupplyChangeNotification n = (MosaicSupplyChangeNotification)notification;
		Assert.assertThat(n.getType(), IsEqual.equalTo(NotificationType.MosaicSupplyChange));
		Assert.assertThat(n.getSupplier(), IsEqual.equalTo(expectedSupplier));
		Assert.assertThat(n.getMosaicId(), IsEqual.equalTo(expectedMosaicId));
		Assert.assertThat(n.getDelta(), IsEqual.equalTo(expectedSupplyChange));
		Assert.assertThat(n.getSupplyType(), IsEqual.equalTo(expectedSupplyType));
	}

	//endregion
}

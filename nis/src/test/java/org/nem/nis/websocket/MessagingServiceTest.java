package org.nem.nis.websocket;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.namespace.NamespaceIdPart;
import org.nem.core.model.ncc.AccountMetaDataPair;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChain;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.harvesting.UnconfirmedState;
import org.nem.nis.harvesting.UnconfirmedTransactionsFilter;
import org.nem.nis.service.AccountInfoFactory;
import org.nem.nis.service.AccountMetaDataFactory;
import org.nem.nis.service.MosaicInfoFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Properties;

@RunWith(Enclosed.class)
public class MessagingServiceTest {

	//region
	public static class MiscForwarding {
		@Test
		public void pushAccountNotifiesProperAccount() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushAccount(address);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(0)).convertAndSend(Mockito.eq("/account"), Mockito.any(AccountMetaDataPair.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/account/" + address.getEncoded()), Mockito.any(AccountMetaDataPair.class));
		}

		@Test
		public void pushTransactionsNotifiesProperAccount() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushTransactions(address, null);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(0)).convertAndSend(Mockito.eq("/recenttransactions"), Mockito.any(SerializableList.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/recenttransactions/" + address.getEncoded()), Mockito.any(SerializableList.class));
		}

		@Test
		public void pushUnconfirmedForwardsToUnconfirmedTransactions() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushUnconfirmed(address);

			// Assert:
			Mockito.verify(testContext.unconfirmedTransactions, Mockito.times(1)).getMostRecentTransactionsForAccount(address, 10);
		}

		@Test
		public void pushOwnedNamespaceForwardsToMosaicInfoFactory() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushOwnedNamespace(address);

			// Assert:
			Mockito.verify(testContext.mosaicInfoFactory, Mockito.times(1)).getAccountOwnedNamespaces(address);
		}

		@Test
		public void pushOwnedMosaicDefinitionForwardsToMosaicInfoFactory() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushOwnedMosaicDefinition(address);

			// Assert:
			Mockito.verify(testContext.mosaicInfoFactory, Mockito.times(1)).getMosaicDefinitionsMetaDataPairs(address);
		}

		@Test
		public void pushOwnedMosaicForwardsToMosaicInfoFactory() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushOwnedMosaic(address);

			// Assert:
			Mockito.verify(testContext.mosaicInfoFactory, Mockito.times(1)).getAccountOwnedMosaics(address);
		}
	}
	//endregion


	//region UnconfirmedTransactionListener
	public static class UnconfirmedTransactionListenerTests {
		@Test
		public void pushingTransactionForwardsToMessagingTemplate() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Transaction transaction = testContext.createTransferTransaction(Utils.generateRandomAccount(), Utils.generateRandomAccount());

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
		}
	}
	//endregion UnconfirmedTransactionListener

	//region pushTransaction (all supported txes)
	private static abstract class AbstractPushTransactionTests {
		protected abstract Transaction wrapTransaction(final Transaction transaction);
		protected abstract void transactionAssert(final TestContext testContext, final Transaction transaction);
		protected abstract void testPrepare(final TestContext testContext);

		@Test
		public void pushTransactionNotifiesTransferTransactionSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createTransferTransaction(sender, Utils.generateRandomAccount()));
			testContext.messagingService.registerAccount(sender.getAddress());

			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Test
		public void pushTransactionNotifiesTransferTransactionRecipient() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createTransferTransaction(Utils.generateRandomAccount(), recipient));
			testContext.messagingService.registerAccount(recipient.getAddress());

			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + recipient.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Test
		public void pushTransactionNotifiesTransferTransactionTwiceIfSenderIsRecipient() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createTransferTransaction(sender, sender));
			testContext.messagingService.registerAccount(sender.getAddress());

			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(2)).convertAndSend(Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Test
		public void pushTransactionNotifiesProvisionNamespaceTransactionSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createProvisionNamespaceTransaction(sender));
			testContext.messagingService.registerAccount(sender.getAddress());

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Test
		public void pushTransactionNotifiesMosaicDefinitionCreationTransactionSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createMosaicDefinitionCreationTransaction(sender, null));
			testContext.messagingService.registerAccount(sender.getAddress());

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Test
		public void pushTransactionNotifiesMosaicDefinitionCreationTransactionLevyRecipient() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account levyRecipient = Utils.generateRandomAccount();
			final MosaicLevy mosaicLevy = testContext.createMosaicLevy(levyRecipient);
			final Transaction transaction = wrapTransaction(testContext.createMosaicDefinitionCreationTransaction(Utils.generateRandomAccount(), mosaicLevy));
			testContext.messagingService.registerAccount(levyRecipient.getAddress());

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + levyRecipient.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Test
		public void pushTransactionNotifiesMosaicSupplyChangeTransactionSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createMosaicSupplyChangeTransaction(sender));
			testContext.messagingService.registerAccount(sender.getAddress());
			testPrepare(testContext);

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
	}

	public static class NormalPushTransactionTests extends AbstractPushTransactionTests {
		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return transaction;
		}

		@Override
		protected void transactionAssert(TestContext testContext, Transaction transaction) {}

		@Override
		protected void testPrepare(TestContext testContext) {}
	}

	public static class MultisigNonObservingPushTransactionTests extends AbstractPushTransactionTests {
		final Account issuer = Utils.generateRandomAccount();

		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return new MultisigTransaction(
					TimeInstant.ZERO,
					issuer,
					transaction
			);
		}

		@Override
		protected void transactionAssert(TestContext testContext, Transaction transaction) {
			Mockito.verify(testContext.messagingTemplate, Mockito.times(0)).convertAndSend(Mockito.eq("/unconfirmed/" + issuer.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Override
		protected void testPrepare(TestContext testContext) {

		}
	}

	public static class MultisigObservingPushTransactionTests extends AbstractPushTransactionTests {
		final Account issuer = Utils.generateRandomAccount();

		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return new MultisigTransaction(
					TimeInstant.ZERO,
					issuer,
					transaction
			);
		}

		@Override
		protected void transactionAssert(TestContext testContext, Transaction transaction) {
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + issuer.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Override
		protected void testPrepare(TestContext testContext) {
			testContext.messagingService.registerAccount(issuer.getAddress());
		}
	}
	//endregion pushTransaction

	//region pushBlock
	private static abstract class AbstractPushBlockTests {
		protected abstract Transaction wrapTransaction(final Transaction transaction);
		protected abstract void transactionAssert(final TestContext testContext, final Transaction transaction);
		protected abstract void testPrepare(final TestContext testContext);

		@Test
		public void pushBlockBroadcastsBlock() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account harvester = Utils.generateRandomAccount();
			final Block block = testContext.createBlock(harvester);

			// Act:
			testContext.messagingService.pushBlock(block);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/blocks", block);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/account/" + harvester.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
		}

		@Test
		public void pushBlockDoesNotNotify() {
			pushBlockNotifiesOnlyRegisteredAccount(false, false);
		}

		@Test
		public void pushBlockNotifiesRegisteredSender() {
			pushBlockNotifiesOnlyRegisteredAccount(true, false);
		}

		@Test
		public void pushBlockNotifiesRegisteredRecipient() {
			pushBlockNotifiesOnlyRegisteredAccount(false, true);
		}

		@Test
		public void pushBlockNotifiesRegisteredAccounts() {
			pushBlockNotifiesOnlyRegisteredAccount(true, true);
		}

		private void pushBlockNotifiesOnlyRegisteredAccount(boolean registerSender, boolean registerRecipient) {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account harvester = Utils.generateRandomAccount();
			final Account sender = Utils.generateRandomAccount();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createTransferTransaction(sender, recipient));
			transaction.sign();

			final Block block = testContext.createBlock(harvester);
			block.addTransaction(transaction);

			if (registerSender) {
				testContext.messagingService.registerAccount(sender.getAddress());
			}
			if (registerRecipient) {
				testContext.messagingService.registerAccount(recipient.getAddress());
			}

			// Act:
			testContext.messagingService.pushBlock(block);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/blocks", block);
			int senderTimes = registerSender ? 1 : 0;
			Mockito.verify(testContext.messagingTemplate, Mockito.times(senderTimes)).convertAndSend(Mockito.eq("/account/" + sender.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
			int recipientTimes = registerRecipient ? 1 : 0;
			Mockito.verify(testContext.messagingTemplate, Mockito.times(recipientTimes)).convertAndSend(Mockito.eq("/account/" + recipient.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));

			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/account/" + harvester.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
		}
	}

	public static class BlockWithNormalTransactions extends AbstractPushBlockTests {
		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return transaction;
		}

		@Override
		protected void transactionAssert(TestContext testContext, Transaction transaction) {}

		@Override
		protected void testPrepare(TestContext testContext) {}
	}
	//endregion pushBlock

	private static class TestContext {
		final BlockChain blockChain = Mockito.mock(BlockChain.class);
		final UnconfirmedState unconfirmedState = Mockito.mock(UnconfirmedState.class);
		final SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
		final AccountInfoFactory accountInfoFactory = Mockito.mock(AccountInfoFactory.class);
		final AccountMetaDataFactory accountMetaDataFactory = Mockito.mock(AccountMetaDataFactory.class);
		final MosaicInfoFactory mosaicInfoFactory = Mockito.mock(MosaicInfoFactory.class);
		final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
		final MessagingService messagingService;

		TestContext() {
			messagingService = new MessagingService(
					blockChain,
					unconfirmedState,
					messagingTemplate,
					accountInfoFactory,
					accountMetaDataFactory,
					mosaicInfoFactory,
					unconfirmedTransactions,
					host
			);
		}

		public Transaction createTransferTransaction(final Account sender, final Account recipient) {
			return new TransferTransaction(
					TimeInstant.ZERO,
					sender,
					recipient,
					Amount.fromNem(100),
					null
			);
		}
		public Transaction createProvisionNamespaceTransaction(final Account sender) {
			return new ProvisionNamespaceTransaction(
					TimeInstant.ZERO,
					sender,
					new NamespaceIdPart("bar"),
					new NamespaceId("fizzbuzz")
			);
		}

		public Transaction createMosaicDefinitionCreationTransaction(final Account sender, final MosaicLevy mosaicLevy) {
			return new MosaicDefinitionCreationTransaction(
					TimeInstant.ZERO,
					sender,
					new MosaicDefinition(
							sender,
							new MosaicId(new NamespaceId("fizzbuzz.bar"), "baz"),
							new MosaicDescriptor("fizzbuzz.bar:baz is a great mosaic, something everyone should have"),
							new DefaultMosaicProperties(new Properties()),
							mosaicLevy
					)
			);
		}

		public MosaicLevy createMosaicLevy(final Account levyRecipient) {
			return new MosaicLevy(
					MosaicTransferFeeType.Percentile,
					levyRecipient,
					new MosaicId(new NamespaceId("fizzbuzz.bar"), "42"),
					Quantity.fromValue(1234L)
			);
		}

		public Transaction createMosaicSupplyChangeTransaction(final Account sender) {
			return new MosaicSupplyChangeTransaction(
					TimeInstant.ZERO,
					sender,
					new MosaicId(new NamespaceId("fizzbuzz.bar"), "baz"),
					MosaicSupplyType.Create,
					Supply.fromValue(1234L)
			);
		}

		public Block createBlock(final Account harvester) {
			return new Block(
					harvester,
					Hash.ZERO,
					Hash.ZERO,
					TimeInstant.ZERO,
					BlockHeight.ONE
			);
		}
	}
}

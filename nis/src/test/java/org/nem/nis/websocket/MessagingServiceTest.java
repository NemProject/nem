package org.nem.nis.websocket;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.namespace.NamespaceIdPart;
import org.nem.core.model.ncc.AccountMetaDataPair;
import org.nem.core.model.ncc.MosaicDefinitionSupplyPair;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.*;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(Enclosed.class)
public class MessagingServiceTest {

	// region misc forwarding

	public static class MiscForwarding {
		@Test
		public void pushAccountNotifiesProperAccount() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushAccount(address);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(0)).convertAndSend(Mockito.eq("/account"),
					Mockito.any(AccountMetaDataPair.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/account/" + address.getEncoded()),
					Mockito.any(AccountMetaDataPair.class));
		}

		@Test
		public void pushTransactionsNotifiesProperAccount() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Address address = Utils.generateRandomAddress();

			// Act:
			testContext.messagingService.pushTransactions(address, null);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(0)).convertAndSend(Mockito.eq("/recenttransactions"),
					Mockito.any(SerializableList.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/recenttransactions/" + address.getEncoded()), Mockito.any(SerializableList.class));
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

	// endregion

	// region UnconfirmedTransactionListener

	public static class UnconfirmedTransactionListenerTests {
		@Test
		public void pushingTransactionForwardsToMessagingTemplate() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Transaction transaction = testContext.createTransferTransaction(Utils.generateRandomAccount(),
					Utils.generateRandomAccount());

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
		}
	}

	// endregion

	// region pushTransaction (all supported txes)

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
			testPrepare(testContext);

			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
		@Test
		public void pushTransactionNotifiesTransferTransactionRecipient() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account recipient = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(
					testContext.createTransferTransaction(Utils.generateRandomAccount(), recipient));
			testContext.messagingService.registerAccount(recipient.getAddress());
			testPrepare(testContext);

			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/unconfirmed/" + recipient.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
		@Test
		public void pushTransactionNotifiesTransferTransactionTwiceIfSenderIsRecipient() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createTransferTransaction(sender, sender));
			testContext.messagingService.registerAccount(sender.getAddress());
			testPrepare(testContext);

			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(2)).convertAndSend(
					Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
		@Test
		public void pushTransactionNotifiesProvisionNamespaceTransactionSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createProvisionNamespaceTransaction(sender));
			testContext.messagingService.registerAccount(sender.getAddress());
			testPrepare(testContext);

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
		@Test
		public void pushTransactionNotifiesMosaicDefinitionCreationTransactionSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createMosaicDefinitionCreationTransaction(sender, null));
			testContext.messagingService.registerAccount(sender.getAddress());
			testPrepare(testContext);

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
		@Test
		public void pushTransactionNotifiesMosaicDefinitionCreationTransactionLevyRecipient() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account levyRecipient = Utils.generateRandomAccount();
			final MosaicLevy mosaicLevy = testContext.createMosaicLevy(levyRecipient);
			final Transaction transaction = wrapTransaction(
					testContext.createMosaicDefinitionCreationTransaction(Utils.generateRandomAccount(), mosaicLevy));
			testContext.messagingService.registerAccount(levyRecipient.getAddress());
			testPrepare(testContext);

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/unconfirmed/" + levyRecipient.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
		@Test
		public void pushTransactionNotifiesMosaicSupplyChangeTransactionSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = wrapTransaction(testContext.createMosaicSupplyChangeTransaction(sender, null));
			testContext.messagingService.registerAccount(sender.getAddress());
			testPrepare(testContext);

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
			transactionAssert(testContext, transaction);
		}
	}

	public static class NormalPushTransactionTests extends AbstractPushTransactionTests {
		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return transaction;
		}

		@Override
		protected void transactionAssert(TestContext testContext, Transaction transaction) {
		}

		@Override
		protected void testPrepare(TestContext testContext) {
		}
	}

	public static class MultisigNonObservingPushTransactionTests extends AbstractPushTransactionTests {
		final Account issuer = Utils.generateRandomAccount();

		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return new MultisigTransaction(TimeInstant.ZERO, issuer, transaction);
		}

		@Override
		protected void transactionAssert(TestContext testContext, Transaction transaction) {
			Mockito.verify(testContext.messagingTemplate, Mockito.times(0)).convertAndSend(
					Mockito.eq("/unconfirmed/" + issuer.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Override
		protected void testPrepare(TestContext testContext) {

		}
	}

	public static class MultisigObservingPushTransactionTests extends AbstractPushTransactionTests {
		final Account issuer = Utils.generateRandomAccount();

		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return new MultisigTransaction(TimeInstant.ZERO, issuer, transaction);
		}

		@Override
		protected void transactionAssert(TestContext testContext, Transaction transaction) {
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/unconfirmed/" + issuer.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}

		@Override
		protected void testPrepare(TestContext testContext) {
			testContext.messagingService.registerAccount(issuer.getAddress());
		}
	}

	// endregion pushTransaction

	// region pushBlock (those tests do not check /transaction/<address>, as they should already be covered by pushTransaction tests)

	private static abstract class AbstractPushBlockTests {
		protected abstract Transaction wrapTransaction(final Transaction transaction);

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
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + harvester.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
		}

		@Test
		public void pushBlockDoesNotNotify() {
			assertPushBlockWithTransferTransaction(false, false);
		}
		@Test
		public void pushBlockWithTransferTransactionNotifiesRegisteredSender() {
			assertPushBlockWithTransferTransaction(true, false);
		}
		@Test
		public void pushBlockWithTransferTransactionNotifiesRegisteredRecipient() {
			assertPushBlockWithTransferTransaction(false, true);
		}
		@Test
		public void pushBlockWithTransferTransactionNotifiesRegisteredAccounts() {
			assertPushBlockWithTransferTransaction(true, true);
		}
		private void assertPushBlockWithTransferTransaction(boolean registerSender, boolean registerRecipient) {
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
			Mockito.verify(testContext.messagingTemplate, Mockito.times(senderTimes))
					.convertAndSend(Mockito.eq("/account/" + sender.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
			int recipientTimes = registerRecipient ? 1 : 0;
			Mockito.verify(testContext.messagingTemplate, Mockito.times(recipientTimes))
					.convertAndSend(Mockito.eq("/account/" + recipient.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));

			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + harvester.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
		}

		@Test
		public void pushBlockWithProvisionNamespaceTransactionNotifiesSender() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account harvester = Utils.generateRandomAccount();
			final Account sender = Utils.generateRandomAccount();
			final ProvisionNamespaceTransaction namespaceTransaction = (ProvisionNamespaceTransaction) testContext
					.createProvisionNamespaceTransaction(sender);
			final Transaction transaction = wrapTransaction(namespaceTransaction);
			transaction.sign();

			final Block block = testContext.createBlock(harvester);
			block.addTransaction(transaction);

			testContext.messagingService.registerAccount(sender.getAddress());
			testContext.addNamespaceEntryInMosaicInfoFactory(namespaceTransaction, sender, 123L);

			// Act:
			testContext.messagingService.pushBlock(block);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/blocks", block);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + sender.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + harvester.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));

			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/account/namespace/owned/" + sender.getAddress().getEncoded()), Mockito.any(Namespace.class));
		}

		@Test
		public void pushBlockWithMosaicDefinitionCreationNotifiesCreator() {
			assertPushBlockWithMosaicDefinitionCreation(false);
		}

		@Test
		public void pushBlockWithMosaicDefinitionCreationNotifiesCreatorAndLevyRecipient() {
			assertPushBlockWithMosaicDefinitionCreation(true);
		}

		private void assertPushBlockWithMosaicDefinitionCreation(boolean withLevy) {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account harvester = Utils.generateRandomAccount();
			final Account sender = Utils.generateRandomAccount();
			final Account levyRecipient = Utils.generateRandomAccount();
			final MosaicLevy mosaicLevy = withLevy ? testContext.createMosaicLevy(levyRecipient) : null;
			final MosaicDefinitionCreationTransaction creationTransaction = (MosaicDefinitionCreationTransaction) testContext
					.createMosaicDefinitionCreationTransaction(sender, mosaicLevy);
			final Transaction transaction = wrapTransaction(creationTransaction);
			transaction.sign();

			final Block block = testContext.createBlock(harvester);
			block.addTransaction(transaction);

			testContext.messagingService.registerAccount(sender.getAddress());
			testContext.addMosaicEntryInMosaicInfoFactory(creationTransaction, sender, 123L);
			if (withLevy) {
				testContext.messagingService.registerAccount(levyRecipient.getAddress());
				testContext.addMosaicEntryInMosaicInfoFactory(creationTransaction, levyRecipient, 234L);
			}

			// Act:
			testContext.messagingService.pushBlock(block);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/blocks", block);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + sender.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + harvester.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));

			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/account/mosaic/owned/definition/" + sender.getAddress().getEncoded()),
					Mockito.any(MosaicDefinitionSupplyPair.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/mosaic/owned/" + sender.getAddress().getEncoded()), Mockito.any(Mosaic.class));
			if (withLevy) {
				Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
						Mockito.eq("/account/mosaic/owned/definition/" + levyRecipient.getAddress().getEncoded()),
						Mockito.any(MosaicDefinitionSupplyPair.class));
				Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
						Mockito.eq("/account/mosaic/owned/" + levyRecipient.getAddress().getEncoded()), Mockito.any(Mosaic.class));
			}
		}

		@Test
		public void pushBlockWithMosaicSupplyNotifiesCreator() {
			assertPushBlockWithMosaicSupply(false);
		}

		@Test
		public void pushBlockWithMosaicSupplyNotifiesCreatorAndLevyRecipient() {
			assertPushBlockWithMosaicSupply(true);
		}

		private void assertPushBlockWithMosaicSupply(boolean withLevy) {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account harvester = Utils.generateRandomAccount();
			final Account sender = Utils.generateRandomAccount();

			// dummy, only to create association using addMosaicEntryInMosaicInfoFactory below
			final Account levyRecipient = Utils.generateRandomAccount();
			final MosaicLevy mosaicLevy = withLevy ? testContext.createMosaicLevy(levyRecipient) : null;

			final MosaicSupplyChangeTransaction supplyTransaction = (MosaicSupplyChangeTransaction) testContext
					.createMosaicSupplyChangeTransaction(sender, mosaicLevy);
			final Transaction transaction = wrapTransaction(supplyTransaction);
			transaction.sign();

			final Block block = testContext.createBlock(harvester);
			block.addTransaction(transaction);

			testContext.messagingService.registerAccount(sender.getAddress());
			final MosaicDefinitionCreationTransaction creationTransaction = (MosaicDefinitionCreationTransaction) testContext
					.createMosaicDefinitionCreationTransaction(sender, mosaicLevy);
			testContext.addMosaicEntryInMosaicInfoFactory(creationTransaction, sender, 123L);
			if (withLevy) {
				testContext.messagingService.registerAccount(levyRecipient.getAddress());
				testContext.addMosaicEntryInMosaicInfoFactory(creationTransaction, levyRecipient, 234L);
			}

			// Act:
			testContext.messagingService.pushBlock(block);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/blocks", block);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + sender.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/" + harvester.getAddress().getEncoded()), Mockito.any(AccountMetaDataPair.class));

			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
					Mockito.eq("/account/mosaic/owned/definition/" + sender.getAddress().getEncoded()),
					Mockito.any(MosaicDefinitionSupplyPair.class));
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1))
					.convertAndSend(Mockito.eq("/account/mosaic/owned/" + sender.getAddress().getEncoded()), Mockito.any(Mosaic.class));

			if (withLevy) {
				Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
						Mockito.eq("/account/mosaic/owned/definition/" + levyRecipient.getAddress().getEncoded()),
						Mockito.any(MosaicDefinitionSupplyPair.class));
				Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(
						Mockito.eq("/account/mosaic/owned/" + levyRecipient.getAddress().getEncoded()), Mockito.any(Mosaic.class));
			}
		}
	}

	public static class BlockWithNormalTransactions extends AbstractPushBlockTests {
		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return transaction;
		}
	}

	public static class BlockWithMultisigTransactions extends AbstractPushBlockTests {
		final Account issuer = Utils.generateRandomAccount();

		@Override
		protected Transaction wrapTransaction(final Transaction transaction) {
			return new MultisigTransaction(TimeInstant.ZERO, issuer, transaction);
		}
	}

	// endregion pushBlock

	// region pushBlocks

	private static class PushBlockTests {
		@Test
		public void pushBlocksNotifiesAboutHeight() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Collection<Block> peerChain = testContext.createBlocks(123, 10);

			// Act:
			testContext.messagingService.pushBlocks(peerChain, new BlockChainScore(1234));

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/blocks/new", new BlockHeight(123));
			for (final Block block : peerChain) {
				Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/block", block);
			}
		}
	}

	// endregion

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
			messagingService = new MessagingService(blockChain, unconfirmedState, messagingTemplate, accountInfoFactory,
					accountMetaDataFactory, mosaicInfoFactory, unconfirmedTransactions, host);
		}

		public Transaction createTransferTransaction(final Account sender, final Account recipient) {
			return new TransferTransaction(TimeInstant.ZERO, sender, recipient, Amount.fromNem(100), null);
		}
		public Transaction createProvisionNamespaceTransaction(final Account sender) {
			return new ProvisionNamespaceTransaction(TimeInstant.ZERO, sender, new NamespaceIdPart("bar"), new NamespaceId("fizzbuzz"));
		}

		public Transaction createMosaicDefinitionCreationTransaction(final Account sender, final MosaicLevy mosaicLevy) {
			return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, sender, testMosaicDefinition(sender, mosaicLevy));
		}

		private MosaicDefinition testMosaicDefinition(final Account sender, final MosaicLevy mosaicLevy) {
			return new MosaicDefinition(sender, new MosaicId(new NamespaceId("fizzbuzz.bar"), "baz"),
					new MosaicDescriptor("fizzbuzz.bar:baz is a great mosaic, something everyone should have"),
					new DefaultMosaicProperties(new Properties()), mosaicLevy);
		}

		public void addMosaicEntryInMosaicInfoFactory(final MosaicDefinitionCreationTransaction mosaicDefinitionCreationTransaction,
				final Account account, long quantity) {
			final MosaicDefinition mosaicDefinition = mosaicDefinitionCreationTransaction.getMosaicDefinition();
			Mockito.when(this.mosaicInfoFactory.getMosaicDefinitionsMetaDataPairs(account.getAddress()))
					.thenReturn(Collections.singleton(new MosaicDefinitionSupplyPair(mosaicDefinition,
							Supply.fromValue(mosaicDefinition.getProperties().getInitialSupply()))));
			Mockito.when(this.mosaicInfoFactory.getAccountOwnedMosaics(account.getAddress()))
					.thenReturn(Collections.singletonList(new Mosaic(mosaicDefinition.getId(), Quantity.fromValue(quantity))));
		}

		public void addNamespaceEntryInMosaicInfoFactory(final ProvisionNamespaceTransaction provisionNamespaceTransaction,
				final Account account, long height) {
			Mockito.when(this.mosaicInfoFactory.getAccountOwnedNamespaces(account.getAddress())).thenReturn(Collections
					.singleton(new Namespace(provisionNamespaceTransaction.getResultingNamespaceId(), account, new BlockHeight(height))));
		}

		public MosaicLevy createMosaicLevy(final Account levyRecipient) {
			return new MosaicLevy(MosaicTransferFeeType.Percentile, levyRecipient, new MosaicId(new NamespaceId("fizzbuzz.bar"), "42"),
					Quantity.fromValue(1234L));
		}

		public Transaction createMosaicSupplyChangeTransaction(final Account sender, final MosaicLevy mosaicLevy) {
			final MosaicId mosaicId = new MosaicId(new NamespaceId("fizzbuzz.bar"), "baz");
			final MosaicDefinition mosaicDefinition = this.testMosaicDefinition(sender, mosaicLevy);
			Mockito.when(this.mosaicInfoFactory.getMosaicDefinition(mosaicId)).thenReturn(mosaicDefinition);

			return new MosaicSupplyChangeTransaction(TimeInstant.ZERO, sender, mosaicId, MosaicSupplyType.Create, Supply.fromValue(1234L));
		}

		public Block createBlock(final Account harvester) {
			return new Block(harvester, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		}

		public Collection<Block> createBlocks(int startingHeight, int len) {
			return IntStream.range(startingHeight, startingHeight + len)
					.mapToObj(i -> new Block(Utils.generateRandomAccount(), Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(i)))
					.collect(Collectors.toList());
		}
	}
}

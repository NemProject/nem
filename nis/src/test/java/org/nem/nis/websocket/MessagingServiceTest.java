package org.nem.nis.websocket;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.namespace.NamespaceIdPart;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.model.primitive.Supply;
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

		@Test
		public void pushingTransactionForwardsToSpecificChannel() {
			// Arrange:
			final TestContext testContext = new TestContext();
			final Account sender = Utils.generateRandomAccount();
			final Transaction transaction = testContext.createTransferTransaction(sender, Utils.generateRandomAccount());
			testContext.messagingService.registerAccount(sender.getAddress());

			// Act:
			testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

			// Assert:
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
			Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
		}
	}
	//endregion UnconfirmedTransactionListener

	//region pushTransaction (all supported txes)
	private static abstract class AbstractPushTransactionTests {
		protected abstract Transaction wrapTransaction(final Transaction transaction);
		protected abstract void transactionAssert(final TestContext testContext, final Transaction transaction);
		protected abstract void testPrepare(final TestContext testContext);

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
	}
}

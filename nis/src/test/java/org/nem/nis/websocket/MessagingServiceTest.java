package org.nem.nis.websocket;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.Transaction;
import org.nem.core.model.TransferTransaction;
import org.nem.core.model.ValidationResult;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.Amount;
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

public class MessagingServiceTest {

	//region UnconfirmedTransactionListener
	@Test
	public void pushingTransactionForwardsToMessagingTemplate() {
		// Arrange:
		final TestContext testContext = new TestContext();
		final Transaction transaction = testContext.createTransferTransaction(Utils.generateRandomAccount());

		// Act:
		testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

		// Assert:
		Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
	}

	@Test
	public void observingForwardsToMessagingTemplate() {
		// Arrange:
		final TestContext testContext = new TestContext();
		final Account sender = Utils.generateRandomAccount();
		final Transaction transaction = testContext.createTransferTransaction(sender);
		testContext.messagingService.registerAccount(sender.getAddress());

		// Act:
		testContext.messagingService.pushTransaction(transaction, ValidationResult.NEUTRAL);

		// Assert:
		Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend("/unconfirmed", transaction);
		Mockito.verify(testContext.messagingTemplate, Mockito.times(1)).convertAndSend(Mockito.eq("/unconfirmed/" + sender.getAddress().getEncoded()), Mockito.any(TransactionMetaDataPair.class));
	}
	//endregion UnconfirmedTransactionListener

	class TestContext {
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

		public Transaction createTransferTransaction(final Account sender) {
			return new TransferTransaction(
					TimeInstant.ZERO,
					sender,
					Utils.generateRandomAccount(),
					Amount.fromNem(100),
					null
			);
		}
	}
}

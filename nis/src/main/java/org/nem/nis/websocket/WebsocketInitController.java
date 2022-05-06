package org.nem.nis.websocket;

import org.nem.core.connect.ErrorResponse;
import org.nem.core.model.Address;
import org.nem.core.model.Block;
import org.nem.core.model.ncc.AccountId;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.TimeProvider;
import org.nem.nis.controller.requests.AccountTransactionsId;
import org.nem.nis.controller.requests.DefaultPage;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.AccountIo;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebsocketInitController {
	private final AccountIo accountIo;
	private final MessagingService messagingService;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final NisDbModelToModelMapper mapper;
	private final TimeProvider timeProvider;

	@Autowired(required = true)
	public WebsocketInitController(final AccountIo accountIo, final MessagingService messagingService,
			final BlockChainLastBlockLayer blockChainLastBlockLayer, final NisDbModelToModelMapper mapper,
			final TimeProvider timeProvider) {
		this.accountIo = accountIo;
		this.messagingService = messagingService;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.mapper = mapper;
		this.timeProvider = timeProvider;
	}

	@MessageMapping("/block/last")
	public void last() throws Exception {
		final Block mapped = this.mapper.map(this.blockChainLastBlockLayer.getLastDbBlock());
		this.messagingService.pushBlock(mapped);
	}

	@MessageMapping("/node/info")
	public void nodeInfo() {
		this.messagingService.pushNodeInfo();
	}

	@MessageMapping("/account/get")
	public void get(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.registerAccount(address);
		this.messagingService.pushAccount(address);
	}

	@MessageMapping("/account/subscribe")
	public void subscribe(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.registerAccount(address);
	}

	@MessageMapping("/account/transfers/all")
	public void transfersAll(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.pushTransactions(address, this.getAccountTransfersUsingId(
				new AccountTransactionsId(address.getEncoded(), null), new DefaultPage(null, null), ReadOnlyTransferDao.TransferType.ALL));

		this.messagingService.pushUnconfirmed(address);
	}

	@MessageMapping("/account/transfers/unconfirmed")
	public void transfersUnconfirmed(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.pushUnconfirmed(address);
	}

	@MessageMapping("/account/mosaic/owned/definition")
	public void accountMosaicOwnedDefinition(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.pushOwnedMosaicDefinition(address);
	}

	@MessageMapping("/account/mosaic/owned")
	public void accountMosaicOwned(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.pushOwnedMosaic(address);
	}

	@MessageMapping("/account/namespace/owned")
	public void accountNamespaceOwned(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.pushOwnedNamespace(address);
	}

	@MessageExceptionHandler
	@SendTo("/errors")
	public ErrorResponse handleException(final Exception exception) {
		exception.printStackTrace();
		return new ErrorResponse(timeProvider.getCurrentTime(), exception.getMessage(), 400);
	}

	private SerializableList<TransactionMetaDataPair> getAccountTransfersUsingId(final AccountTransactionsId id, final DefaultPage page,
			final ReadOnlyTransferDao.TransferType transferType) {
		if (null != page.getId()) {
			return this.accountIo.getAccountTransfersUsingId(id.getAddress(), page.getId(), transferType, page.getPageSize());
		}

		return this.accountIo.getAccountTransfersUsingId(id.getAddress(), null, transferType, page.getPageSize());
	}
}

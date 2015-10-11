package org.nem.nis.websocket;

import org.nem.core.connect.ErrorResponse;
import org.nem.core.model.AccountRemoteStatus;
import org.nem.core.model.Address;
import org.nem.core.model.Block;
import org.nem.core.model.ncc.AccountId;
import org.nem.core.model.ncc.AccountMetaData;
import org.nem.core.model.ncc.AccountMetaDataPair;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.AccountInfoFactory;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.ReadOnlyAccountState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * whole controller could be removed, but it can be useful for testing
 */
@Controller
public class WebsocketInitController {
	private MessagingService messagingService;
	private BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final NisDbModelToModelMapper mapper;
	private final TimeProvider timeProvider;

	@Autowired(required = true)
	public WebsocketInitController(
			final MessagingService messagingService,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final NisDbModelToModelMapper mapper,
			final TimeProvider timeProvider) {
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

	@MessageMapping("/account/get")
	public void get(@Payload final AccountId accountId) {
		final Address address = accountId.getAddress();
		this.messagingService.pushAccount(address);
	}

	@MessageExceptionHandler
	@SendTo("/errors")
	public ErrorResponse handleException(final Exception exception) {
		exception.printStackTrace();
		return new ErrorResponse(timeProvider.getCurrentTime(), exception.getMessage(), 400);
	}
}
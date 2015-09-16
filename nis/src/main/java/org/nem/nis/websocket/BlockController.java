package org.nem.nis.websocket;

import org.nem.core.model.Block;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * whole controller could be removed, but it can be useful for testing
 */
@Controller
public class BlockController {
	private MessagingService messagingService;

	private BlockChainLastBlockLayer lastBlockLayer;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	public BlockController(
			final MessagingService messagingService,
			final BlockChainLastBlockLayer lastBlockLayer,
			final NisDbModelToModelMapper mapper) {
		this.messagingService = messagingService;
		this.lastBlockLayer = lastBlockLayer;
		this.mapper = mapper;
	}

	@MessageMapping("/block/last")
	public void last() throws Exception {
		final Block mapped = this.mapper.map(this.lastBlockLayer.getLastDbBlock());
		this.messagingService.pushBlock(mapped);
	}
}
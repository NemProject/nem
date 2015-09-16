package org.nem.nis.websocket;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChain;
import org.nem.nis.service.BlockListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;


@Service
public class MessagingService implements BlockListener {
	private final SimpMessagingTemplate messagingTemplate;
	private final BlockChain blockChain;

	@Autowired
	public MessagingService(
			final SimpMessagingTemplate messagingTemplate,
	        final BlockChain blockChain)
	{
		this.messagingTemplate = messagingTemplate;
		this.blockChain = blockChain;

		this.blockChain.addListener(this);
	}

	public void pushBlock(final Block block) {
		this.messagingTemplate.convertAndSend("/blocks", block);
	}

	@Override
	public void pushBlocks(final Collection<Block> peerChain, final BlockChainScore peerScore) {
		peerChain.forEach(this::pushBlock);
	}
}

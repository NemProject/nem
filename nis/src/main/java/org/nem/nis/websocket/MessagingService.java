package org.nem.nis.websocket;

import org.nem.core.model.Block;
import org.nem.core.model.Transaction;
import org.nem.core.model.ValidationResult;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.nis.BlockChain;
import org.nem.nis.harvesting.UnconfirmedState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;


@Service
public class MessagingService implements BlockListener, UnconfirmedTransactionListener {
	private final SimpMessagingTemplate messagingTemplate;
	private final BlockChain blockChain;
	private final UnconfirmedState unconfirmedState;

	@Autowired
	public MessagingService(
			final SimpMessagingTemplate messagingTemplate,
			final BlockChain blockChain,
			final UnconfirmedState unconfirmedState)
	{
		this.messagingTemplate = messagingTemplate;
		this.blockChain = blockChain;
		this.unconfirmedState = unconfirmedState;

		this.blockChain.addListener(this);
		this.unconfirmedState.addListener(this);
	}

	public void pushBlock(final Block block) {
		this.messagingTemplate.convertAndSend("/blocks", block);
	}

	@Override
	public void pushBlocks(final Collection<Block> peerChain, final BlockChainScore peerScore) {
		peerChain.forEach(this::pushBlock);
	}

	@Override
	public void pushTransaction(final Transaction transaction, final ValidationResult validationResult) {
		this.messagingTemplate.convertAndSend("/unconfirmed", transaction);
	}
}

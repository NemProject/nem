package org.nem.nis.controller;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.AuthenticatedBlockHeightRequest;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.service.*;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
public class ChainController {
	private static final Logger LOGGER = Logger.getLogger(ChainController.class.getName());

	private final AccountLookup accountLookup;
	private final ReadOnlyBlockDao blockDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	public ChainController(
			final ReadOnlyBlockDao blockDao,
			final AccountLookup accountLookup,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockChain blockChain,
			final NisPeerNetworkHost host) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockChain = blockChain;
		this.host = host;
	}

	//region blockLast

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.GET)
	@ClientApi
	public Block blockLast() {
		final Block block = BlockMapper.toModel(this.blockChainLastBlockLayer.getLastDbBlock(), this.accountLookup);
		LOGGER.info("/chain/last-block height:" + block.getHeight() + " signer:" + block.getSigner());
		return block;
	}

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<Block> blockLast(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.blockLast(), localNode.getIdentity(), challenge);
	}

	//endregion

	@RequestMapping(value = "/chain/blocks-after", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<SerializableList<Block>> blocksAfter(@RequestBody final AuthenticatedBlockHeightRequest request) {
		final Collection<Block> blocks =
				this.blockDao.getBlocksAfter(request.getEntity().getRaw(), BlockChainConstants.BLOCKS_LIMIT).stream()
				.map(dbBlock -> BlockMapper.toModel(dbBlock, this.accountLookup))
				.collect(Collectors.toList());

		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(
				new SerializableList<>(blocks),
				localNode.getIdentity(),
				request.getChallenge());
	}

	@RequestMapping(value = "/chain/hashes-from", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<HashChain> hashesFrom(@RequestBody final AuthenticatedBlockHeightRequest request) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(
				this.blockDao.getHashesFrom(request.getEntity(), BlockChainConstants.BLOCKS_LIMIT),
				localNode.getIdentity(),
				request.getChallenge());
	}

	//region chainScore

	@RequestMapping(value = "/chain/score", method = RequestMethod.GET)
	@PublicApi
	public BlockChainScore chainScore() {
		return this.blockChain.getScore();
	}

	@RequestMapping(value = "/chain/score", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<BlockChainScore> chainScore(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.chainScore(), localNode.getIdentity(), challenge);
	}

	//endregion

	//region chainHeight

	@RequestMapping(value = "/chain/height", method = RequestMethod.GET)
	@PublicApi
	public BlockHeight chainHeight() {
		return new BlockHeight(this.blockChainLastBlockLayer.getLastBlockHeight());
	}

	@RequestMapping(value = "/chain/height", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<BlockHeight> chainHeight(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.chainHeight(), localNode.getIdentity(), challenge);
	}

	//endregion
}

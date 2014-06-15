package org.nem.nis.controller;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.core.serialization.*;
import org.nem.nis.BlockChain;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.viewmodels.AuthenticatedBlockHeightRequest;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.service.RequiredBlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChainController {

	private final AccountLookup accountLookup;
	private final RequiredBlockDao blockDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	public ChainController(
			final RequiredBlockDao blockDao, 
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
	@PublicApi
	public Block blockLast() {
		return BlockMapper.toModel(this.blockChainLastBlockLayer.getLastDbBlock(), this.accountLookup);
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
	public AuthenticatedResponse<SerializableList<Block>> blocksAfter(
			@RequestBody final AuthenticatedBlockHeightRequest request) {
		// TODO: add tests for this action
		org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(request.getEntity());
		final SerializableList<Block> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		for (int i = 0; i < BlockChainConstants.BLOCKS_LIMIT; ++i) {
			final Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			blockList.add(BlockMapper.toModel(dbBlock, this.accountLookup));
		}

		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(
				blockList,
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
	@PublicApi
	@AuthenticatedApi
	public AuthenticatedResponse<BlockChainScore> chainScore(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.chainScore(), localNode.getIdentity(), challenge);
	}

	//endregion
}

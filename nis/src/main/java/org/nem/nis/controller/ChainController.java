package org.nem.nis.controller;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.logging.Logger;

@RestController
public class ChainController {
	private static final Logger LOGGER = Logger.getLogger(ChainController.class.getName());

	private final AccountLookup accountLookup;
	private final ReadOnlyBlockDao blockDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockChainScoreManager blockChainScoreManager;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	public ChainController(
			final ReadOnlyBlockDao blockDao,
			final AccountLookup accountLookup,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockChainScoreManager blockChainScoreManager,
			final NisPeerNetworkHost host) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockChainScoreManager = blockChainScoreManager;
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
		return variableBlocksAfter(new AuthenticatedChainRequest(new ChainRequest(request.getEntity()), request.getChallenge()));
	}

	@RequestMapping(value = "/chain/variable-blocks-after", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<SerializableList<Block>> variableBlocksAfter(@RequestBody final AuthenticatedChainRequest request) {
		final long start = System.currentTimeMillis();
		final ChainRequest chainRequest = request.getEntity();
		int numBlocks = Math.min(BlockChainConstants.BLOCKS_LIMIT, chainRequest.getMinBlocks() + 100); // TODO 20141122 J-B: i think this should be a function on ChainRequest
		final SerializableList<Block> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		boolean enough = addBlocks(blockList, chainRequest.getHeight(), numBlocks, chainRequest.getMaxTransactions());
		numBlocks = 100;
		while (!enough) {
			enough = addBlocks(blockList, blockList.get(blockList.size() - 1).getHeight(), numBlocks, chainRequest.getMaxTransactions());
		}

		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Pulling %d blocks from db starting at height %d needed %dms.",
				blockList.size(),
				chainRequest.getHeight().getRaw(),
				stop - start));
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(
				blockList,
				localNode.getIdentity(),
				request.getChallenge());
	}

	private boolean addBlocks(
			final SerializableList<Block> blockList,
			final BlockHeight height,
			final int numBlocksToRequest,
			final int maxTransactions) {
		int numTransactions = blockList.asCollection().stream().map(b -> b.getTransactions().size()).reduce(0, Integer::sum);
		final Collection<org.nem.nis.dbmodel.Block> dbBlockList = this.blockDao.getBlocksAfter(height, numBlocksToRequest);
		if (dbBlockList.isEmpty()) {
			return true;
		}

		org.nem.nis.dbmodel.Block previousDbBlock = null;
		for (final org.nem.nis.dbmodel.Block dbBlock : dbBlockList) {
			// There should be only one block per height. Just to be sure everything is fine we make this check.
			if (null != previousDbBlock && (previousDbBlock.getHeight() + 1 != dbBlock.getHeight())) {
				throw new RuntimeException("Corrupt block list returned from db.");
			}

			previousDbBlock = dbBlock;
			numTransactions += dbBlock.getBlockImportanceTransfers().size() + dbBlock.getBlockTransfers().size();
			if (numTransactions > maxTransactions || BlockChainConstants.BLOCKS_LIMIT <= blockList.size()) {
				return true;
			}

			blockList.add(BlockMapper.toModel(dbBlock, this.accountLookup));
		}

		return false;
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
		return this.blockChainScoreManager.getScore();
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

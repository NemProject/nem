package org.nem.nis.controller;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.peer.node.*;
import org.nem.peer.requests.ChainRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.logging.Logger;

@RestController
public class ChainController {
	private static final Logger LOGGER = Logger.getLogger(ChainController.class.getName());

	private final ReadOnlyBlockDao blockDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockChainScoreManager blockChainScoreManager;
	private final NisPeerNetworkHost host;
	private final NisDbModelToModelMapper mapper;
	private final int blocksLimit;

	@Autowired(required = true)
	public ChainController(final ReadOnlyBlockDao blockDao, final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockChainScoreManager blockChainScoreManager, final NisPeerNetworkHost host, final NisDbModelToModelMapper mapper) {
		this.blockDao = blockDao;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockChainScoreManager = blockChainScoreManager;
		this.host = host;
		this.mapper = mapper;
		this.blocksLimit = NemGlobals.getBlockChainConfiguration().getMaxBlocksPerSyncAttempt();
	}

	// region blockLast

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.GET)
	@ClientApi
	public Block blockLast() {
		final Block block = this.mapper.map(this.blockChainLastBlockLayer.getLastDbBlock());
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

	// endregion

	@RequestMapping(value = "/chain/blocks-after", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<SerializableList<Block>> blocksAfter(@RequestBody final AuthenticatedChainRequest request) {
		final long start = System.currentTimeMillis();
		final ChainRequest chainRequest = request.getEntity();
		int numBlocks = chainRequest.getNumBlocks();
		final SerializableList<Block> blockList = new SerializableList<>(this.blocksLimit);
		boolean enough = this.addBlocks(blockList, chainRequest.getHeight(), numBlocks, chainRequest.getMaxTransactions());
		numBlocks = 100;
		while (!enough) {
			enough = this.addBlocks(blockList, blockList.get(blockList.size() - 1).getHeight(), numBlocks,
					chainRequest.getMaxTransactions());
		}

		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Pulling %d blocks from db starting at height %d needed %dms.", blockList.size(),
				chainRequest.getHeight().getRaw(), stop - start));
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(blockList, localNode.getIdentity(), request.getChallenge());
	}

	private boolean addBlocks(final SerializableList<Block> blockList, final BlockHeight height, final int numBlocksToRequest,
			final int maxTransactions) {
		int numTransactions = blockList.asCollection().stream().map(b -> b.getTransactions().size()).reduce(0, Integer::sum);
		final Collection<DbBlock> dbBlockList = this.blockDao.getBlocksAfter(height, numBlocksToRequest);
		if (dbBlockList.isEmpty()) {
			return true;
		}

		DbBlock previousDbBlock = null;
		for (final DbBlock dbBlock : dbBlockList) {
			// There should be only one block per height. Just to be sure everything is fine we make this check.
			if (null != previousDbBlock && (previousDbBlock.getHeight() + 1 != dbBlock.getHeight())) {
				throw new RuntimeException("Corrupt block list returned from db.");
			}

			previousDbBlock = dbBlock;
			numTransactions += DbBlockExtensions.countTransactions(dbBlock);

			if (numTransactions > maxTransactions || this.blocksLimit <= blockList.size()) {
				return true;
			}

			blockList.add(this.mapper.map(dbBlock));
		}

		return false;
	}

	@RequestMapping(value = "/chain/hashes-from", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<HashChain> hashesFrom(@RequestBody final AuthenticatedBlockHeightRequest request) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.blockDao.getHashesFrom(request.getEntity(), this.blocksLimit), localNode.getIdentity(),
				request.getChallenge());
	}

	// region chainScore

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

	// endregion

	// region chainHeight

	@RequestMapping(value = "/chain/height", method = RequestMethod.GET)
	@PublicApi
	public BlockHeight chainHeight() {
		return this.blockChainLastBlockLayer.getLastBlockHeight();
	}

	@RequestMapping(value = "/chain/height", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<BlockHeight> chainHeight(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.chainHeight(), localNode.getIdentity(), challenge);
	}

	// endregion
}

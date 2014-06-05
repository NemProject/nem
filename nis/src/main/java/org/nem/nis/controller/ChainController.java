package org.nem.nis.controller;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.BlockChain;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.service.RequiredBlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChainController {

	private AccountLookup accountLookup;
	private RequiredBlockDao blockDao;
	private BlockChainLastBlockLayer blockChainLastBlockLayer;
	private BlockChain blockChain;

	@Autowired(required = true)
	public ChainController(
			final RequiredBlockDao blockDao, 
			final AccountLookup accountLookup, 
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockChain blockChain) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockChain = blockChain;
	}

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public Block blockLast() {
		return BlockMapper.toModel(this.blockChainLastBlockLayer.getLastDbBlock(), this.accountLookup);
	}

	@RequestMapping(value = "/chain/blocks-after", method = RequestMethod.POST)
	@P2PApi
	public SerializableList<Block> blocksAfter(@RequestBody final BlockHeight height) {
		// TODO: add tests for this action
		org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(height);
		final SerializableList<Block> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		for (int i = 0; i < BlockChainConstants.BLOCKS_LIMIT; ++i) {
			Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			blockList.add(BlockMapper.toModel(dbBlock, this.accountLookup));
		}

		return blockList;
	}

	@RequestMapping(value = "/chain/hashes-from", method = RequestMethod.POST)
	@P2PApi
	public HashChain hashesFrom(@RequestBody final BlockHeight height) {
		return this.blockDao.getHashesFrom(height, BlockChainConstants.BLOCKS_LIMIT);
	}

	@RequestMapping(value = "/chain/score", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public BlockChainScore chainScore() {
		return this.blockChain.getScore();
	}
}

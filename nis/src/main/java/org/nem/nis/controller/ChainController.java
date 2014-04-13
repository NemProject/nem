package org.nem.nis.controller;

import org.nem.core.model.HashChain;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.utils.RequiredBlockDaoAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.core.model.Block;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;

@RestController
public class ChainController {

	private AccountLookup accountLookup;
	private RequiredBlockDaoAdapter blockDao;
	private BlockChain blockChain;

	@Autowired(required = true)
	ChainController(final RequiredBlockDaoAdapter blockDao, final AccountLookup accountLookup, BlockChain blockChain) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
		this.blockChain = blockChain;
	}

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public Block blockLast() {
		return BlockMapper.toModel(this.blockChain.getLastDbBlock(), this.accountLookup);
	}

	@RequestMapping(value = "/chain/blocks-after", method = RequestMethod.POST)
	@P2PApi
	public String blocksAfter(@RequestBody final Deserializer deserializer) {
		// TODO: add tests for this action
		final Long blockHeight = deserializer.readLong("height");
		org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);
		final List<Block> blockList = new LinkedList<>();
		for (int i = 0; i < BlockChain.ESTIMATED_BLOCKS_PER_DAY / 2; ++i) {
			Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			blockList.add(BlockMapper.toModel(dbBlock, this.accountLookup));
		}

		// TODO: add converter
		final JsonSerializer serializer = new JsonSerializer();
		serializer.writeObjectArray("blocks", blockList);
		return serializer.getObject().toString() + "\r\n";
	}

	@RequestMapping(value = "/chain/hashes-from", method = RequestMethod.POST)
	@P2PApi
	public HashChain hashesFrom(@RequestBody final Deserializer deserializer) {
		final Long blockHeight = deserializer.readLong("height");
		return this.blockDao.getHashesFrom(blockHeight, BlockChain.BLOCKS_LIMIT);
	}
}

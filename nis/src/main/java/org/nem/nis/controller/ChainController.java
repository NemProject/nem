package org.nem.nis.controller;

import org.nem.core.model.HashChain;
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

	private AccountAnalyzer accountAnalyzer;
	private RequiredBlockDaoAdapter blockDao;
	private BlockChain blockChain;

	@Autowired(required = true)
	ChainController(final RequiredBlockDaoAdapter blockDao, final AccountAnalyzer accountAnalyzer, BlockChain blockChain) {
		this.blockDao = blockDao;
		this.accountAnalyzer = accountAnalyzer;
		this.blockChain = blockChain;
	}

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public Block blockLast(@RequestHeader(value="Accept") String acceptHeader) {
		return BlockMapper.toModel(this.blockChain.getLastDbBlock(), this.accountAnalyzer);
	}

	@RequestMapping(value = "/chain/blocks-after", method = RequestMethod.POST)
	@P2PApi
	public String blocksAfter(@RequestBody final Deserializer deserializer) {
		final Long blockHeight = deserializer.readLong("height");
		org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);
		final List<Block> blockList = new LinkedList<>();
		for (int i = 0; i < BlockChain.ESTIMATED_BLOCKS_PER_DAY / 2; ++i) {
			Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			blockList.add(BlockMapper.toModel(dbBlock, this.accountAnalyzer));
		}

		// TODO: add converter
		final JsonSerializer serializer = new JsonSerializer();
		serializer.writeObjectArray("blocks", blockList);
		return serializer.getObject().toString() + "\r\n";
	}

	@RequestMapping(value = "/chain/hashes-from", method = RequestMethod.POST)
	@P2PApi
	public String hashesFrom(@RequestBody final Deserializer deserializer) {
		final Long blockHeight = deserializer.readLong("height");
		final org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);

		final List<byte[]> hashesList = this.blockDao.getHashesFrom(blockHeight, BlockChain.BLOCKS_LIMIT);
		final HashChain hashChain = new HashChain(hashesList.size());
        for (final byte[] hash : hashesList)
			hashChain.add(hash);

		final JsonSerializer serializer = new JsonSerializer();
		serializer.writeObject("hashchain", hashChain);
		return serializer.getObject().toString() + "\r\n";
	}
}

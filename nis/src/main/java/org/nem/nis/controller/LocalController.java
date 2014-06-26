package org.nem.nis.controller;

import org.nem.core.model.Block;
import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.ncc.BlockMetaData;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.ncc.BlockMetaDataPair;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.service.RequiredBlockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalController {
	private final RequiredBlockDao blockDao;
	private final AccountLookup accountLookup;


	@Autowired(required = true)
	public LocalController(
			final RequiredBlockDao blockDao,
			final AccountLookup accountLookup) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
	}

	@RequestMapping(value = "/local/chain/blocks-after", method = RequestMethod.POST)
	@ClientApi
	public SerializableList<BlockMetaDataPair> localBlocksAfter(@RequestBody final BlockHeight height) {
		// TODO: add tests for this action
		org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(height);
		final SerializableList<BlockMetaDataPair> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		for (int i = 0; i < BlockChainConstants.BLOCKS_LIMIT; ++i) {
			final Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			Block block = BlockMapper.toModel(dbBlock, this.accountLookup);
			blockList.add(new BlockMetaDataPair(block, new BlockMetaData(dbBlock.getBlockHash())));
		}

		return blockList;
	}


}

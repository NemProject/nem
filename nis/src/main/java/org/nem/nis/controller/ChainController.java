package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.nem.core.dao.BlockDao;
import org.nem.core.mappers.BlockMapper;
import org.nem.core.model.Block;
import org.nem.core.model.ByteArray;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@RestController
public class ChainController {

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private BlockChain blockChain;

	@RequestMapping(value = "/chain/last-block", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public String blockLast() {
		final Block lastBlock = BlockMapper.toModel(this.blockChain.getLastDbBlock(), this.accountAnalyzer);
		return ControllerUtils.serialize(lastBlock);
	}

	@RequestMapping(value = "/chain/blocks-after", method = RequestMethod.POST)
	@P2PApi
	public String blocksAfter(@RequestBody final String body) {
		final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
		Long blockHeight = deserializer.readLong("height");

		org.nem.core.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);
		if (null == dbBlock)
			return Utils.jsonError(2, "block not found in the db");

		List<Block> blockList = new LinkedList<Block>();
		for (int i = 0; i < blockChain.ESTIMATED_BLOCKS_PER_DAY / 2; ++i) {
			Long curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				break;
			}
			dbBlock = this.blockDao.findById(curBlockId);
			blockList.add(BlockMapper.toModel(dbBlock, this.accountAnalyzer));
		}

		if (0 == blockList.size())
			return Utils.jsonError(3, "invalid call");

		JsonSerializer serializer = new JsonSerializer();
		serializer.writeObjectArray("blocks", blockList);
		return serializer.getObject().toString() + "\r\n";
	}

	@RequestMapping(value = "/chain/hashes-from", method = RequestMethod.POST)
	@P2PApi
	public String hashesFrom(@RequestBody final String body) {
		final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
		Long blockHeight = deserializer.readLong("height");

		org.nem.core.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);
		if (null == dbBlock) {
			return Utils.jsonError(2, "block not found in the db");
		}

		List<byte[]> hashesList = this.blockDao.getHashesFrom(blockHeight, BlockChain.BLOCKS_LIMIT);
		if (0 == hashesList.size())
			return Utils.jsonError(3, "invalid call");

        List<ByteArray> byteArrayList = new ArrayList<>(hashesList.size());
        for (int i = 0; i < hashesList.size(); ++i) {
            byteArrayList.set(i, new ByteArray(hashesList.get(i)));
        }

		JsonSerializer serializer = new JsonSerializer();
		serializer.writeObjectArray("hashes", byteArrayList);
		return serializer.getObject().toString() + "\r\n";
	}
}

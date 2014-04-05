package org.nem.nis.controller;

import org.apache.commons.codec.DecoderException;
import org.nem.nis.dao.BlockDao;

import org.nem.nis.mappers.BlockMapper;
import org.nem.core.model.Block;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class BlockController {

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	/**
	 * Obtain block from the block chain.
	 *
	 * @param blockHashString hash of a block
	 *
	 * @return block along with associated elements.
	 */
	@RequestMapping(value = "/block/get", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public String blockGet(@RequestParam(value = "blockHash") final String blockHashString) throws DecoderException {
		final byte[] blockHash = HexEncoder.getBytes(blockHashString);
		final org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHash(blockHash);
		if (null == dbBlock)
			return Utils.jsonError(2, "hash not found in the db");

		final Block block = BlockMapper.toModel(dbBlock, this.accountAnalyzer);
		return ControllerUtils.serialize(block);
	}

	@RequestMapping(value = "/block/at", method = RequestMethod.POST)
	@P2PApi
	public String blockAt(@RequestBody final String body) {
		final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
		Long blockHeight = deserializer.readLong("height");

		final org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);
		if (null == dbBlock)
			return Utils.jsonError(2, "block not found in the db");

		final Block block = BlockMapper.toModel(dbBlock, this.accountAnalyzer);
		return ControllerUtils.serialize(block);
	}
}

package org.nem.nis.controller;

import org.apache.commons.codec.DecoderException;
import org.nem.core.model.Hash;
import org.nem.nis.dao.BlockDao;

import org.nem.nis.mappers.BlockMapper;
import org.nem.core.model.Block;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.MissingResourceException;

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
	public Block blockGet(@RequestParam(value = "blockHash") final String blockHashString) throws DecoderException {
		final Hash blockHash = new Hash(HexEncoder.getBytes(blockHashString));
		final org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHash(blockHash);
		if (null == dbBlock)
			throw new MissingResourceException("hash not found in the db", Block.class.getName(), blockHashString);

		return BlockMapper.toModel(dbBlock, this.accountAnalyzer);
	}

	@RequestMapping(value = "/block/at", method = RequestMethod.POST)
	@P2PApi
	public Block blockAt(@RequestBody final String body) {
		final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
		final Long blockHeight = deserializer.readLong("height");

		final org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);
		if (null == dbBlock)
			throw new MissingResourceException("block not found in the db", Block.class.getName(), blockHeight.toString());

		return BlockMapper.toModel(dbBlock, this.accountAnalyzer);
	}
}

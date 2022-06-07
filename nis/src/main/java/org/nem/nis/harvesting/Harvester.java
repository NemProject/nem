package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.time.*;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;

import java.util.logging.Logger;

public class Harvester {
	private static final Logger LOGGER = Logger.getLogger(Harvester.class.getName());
	private final TimeProvider timeProvider;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final UnlockedAccounts unlockedAccounts;
	private final NisDbModelToModelMapper mapper;
	private final BlockGenerator generator;

	/**
	 * Creates a new harvester.
	 *
	 * @param timeProvider The time provider.
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 * @param unlockedAccounts The unlocked accounts.
	 * @param mapper The mapper.
	 * @param generator The block generator.
	 */
	public Harvester(final TimeProvider timeProvider, final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final UnlockedAccounts unlockedAccounts, final NisDbModelToModelMapper mapper, final BlockGenerator generator) {
		this.timeProvider = timeProvider;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.unlockedAccounts = unlockedAccounts;
		this.mapper = mapper;
		this.generator = generator;
	}

	/**
	 * Returns harvested block or null.
	 *
	 * @return Best block that could be created by unlocked accounts.
	 */
	public Block harvestBlock() {
		final TimeInstant blockTime = this.timeProvider.getCurrentTime();
		this.unlockedAccounts.prune(this.blockChainLastBlockLayer.getLastBlockHeight().next());
		if (this.blockChainLastBlockLayer.isLoading() || this.unlockedAccounts.size() == 0) {
			return null;
		}

		final DbBlock dbLastBlock = this.blockChainLastBlockLayer.getLastDbBlock();
		final Block lastBlock = this.mapper.map(dbLastBlock);

		// can be null inside tests
		if (dbLastBlock != null) {
			LOGGER.info(String.format("db block height: %d", dbLastBlock.getHeight()));
			LOGGER.info(
					String.format("db block hash: %s", dbLastBlock.getBlockHash().getRaw() == null ? "null" : dbLastBlock.getBlockHash()));
		}
		LOGGER.info(String.format("%d harvesters are attempting to harvest a new block.", this.unlockedAccounts.size()));
		GeneratedBlock bestGeneratedBlock = null;
		for (final Account harvester : this.unlockedAccounts) {
			final GeneratedBlock generatedBlock = this.generator.generateNextBlock(lastBlock, harvester, blockTime);

			if (null != generatedBlock && (null == bestGeneratedBlock || generatedBlock.getScore() > bestGeneratedBlock.getScore())) {
				bestGeneratedBlock = generatedBlock;
			}
		}

		return null == bestGeneratedBlock ? null : bestGeneratedBlock.getBlock();
	}
}

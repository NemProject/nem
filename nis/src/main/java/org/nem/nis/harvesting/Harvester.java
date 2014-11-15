package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.*;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;

public class Harvester {
	private final AccountLookup accountLookup;
	private final TimeProvider timeProvider;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final UnlockedAccounts unlockedAccounts;
	private final BlockGenerator generator;

	/**
	 * Creates a new harvester.
	 *
	 * @param accountLookup The account lookup.
	 * @param timeProvider The time provider.
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 * @param unlockedAccounts The unlocked accounts.
	 */
	public Harvester(
			final AccountLookup accountLookup,
			final TimeProvider timeProvider,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final UnlockedAccounts unlockedAccounts,
			final BlockGenerator generator) {
		this.accountLookup = accountLookup;
		this.timeProvider = timeProvider;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.unlockedAccounts = unlockedAccounts;
		this.generator = generator;
	}

	/**
	 * Returns harvested block or null.
	 *
	 * @return Best block that could be created by unlocked accounts.
	 */
	public Block harvestBlock() {
		if (this.blockChainLastBlockLayer.getLastDbBlock() == null || this.unlockedAccounts.size() == 0) {
			return null;
		}

		final TimeInstant blockTime = this.timeProvider.getCurrentTime();
		final org.nem.nis.dbmodel.Block dbLastBlock = this.blockChainLastBlockLayer.getLastDbBlock();
		final Block lastBlock = BlockMapper.toModel(dbLastBlock, this.accountLookup);

		GeneratedBlock bestGeneratedBlock = null;
		for (final Account harvester : this.unlockedAccounts) {
			final GeneratedBlock generatedBlock = this.generator.generateNextBlock(
					lastBlock,
					harvester,
					blockTime);

			if (null != generatedBlock && (null == bestGeneratedBlock || generatedBlock.getScore() > bestGeneratedBlock.getScore())) {
				bestGeneratedBlock = generatedBlock;
			}
		}

		return null == bestGeneratedBlock ? null : bestGeneratedBlock.getBlock();
	}
}

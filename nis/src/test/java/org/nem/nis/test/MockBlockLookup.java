package org.nem.nis.test;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.sync.BlockLookup;

import java.util.*;

/**
 * A mock BlockLookup implementation.
 */
public class MockBlockLookup implements BlockLookup {

	private final Block lastBlock;
	private final BlockChainScore chainScore;
	private final HashChain chain;
	private final Map<BlockHeight, Block> heightToBlockMap = new HashMap<>();

	/**
	 * Creates a new mock block lookup.
	 *
	 * @param lastBlock The last block.
	 */
	public MockBlockLookup(final Block lastBlock) {
		this(lastBlock, 1);
	}

	/**
	 * Creates a new mock block lookup.
	 *
	 * @param lastBlock The last block.
	 * @param chainScore The chain score.
	 */
	public MockBlockLookup(final Block lastBlock, final BlockChainScore chainScore) {
		this(lastBlock, chainScore, 1);
	}

	/**
	 * Creates a new mock block lookup.
	 *
	 * @param lastBlock The last block.
	 * @param numHashesToReturn The number of hashes to return from getHashesFrom.
	 */
	public MockBlockLookup(final Block lastBlock, final int numHashesToReturn) {
		this(lastBlock, new BlockChainScore(1), numHashesToReturn);
	}

	/**
	 * Creates a new mock block lookup.
	 *
	 * @param lastBlock The last block.
	 * @param chainScore The chain score.
	 * @param numHashesToReturn The number of hashes to return from getHashesFrom.
	 */
	public MockBlockLookup(final Block lastBlock, final BlockChainScore chainScore, final int numHashesToReturn) {
		this.lastBlock = lastBlock;
		this.chainScore = chainScore;

		this.chain = new HashChain(numHashesToReturn);
		for (int i = 0; i < numHashesToReturn; ++i) {
			this.chain.add(Utils.generateRandomHash());
		}
	}

	/**
	 * Creates a new mock block lookup.
	 *
	 * @param lastBlock The last block.
	 * @param hashChain The hashes to return from getHashesFrom.
	 */
	public MockBlockLookup(final Block lastBlock, final HashChain hashChain) {
		this(lastBlock, new BlockChainScore(1), hashChain);
	}

	/**
	 * Creates a new mock block lookup.
	 *
	 * @param lastBlock The last block.
	 * @param chainScore The chain score.
	 * @param hashChain The hashes to return from getHashesFrom.
	 */
	public MockBlockLookup(final Block lastBlock, final BlockChainScore chainScore, final HashChain hashChain) {
		this.lastBlock = lastBlock;
		this.chain = hashChain;
		this.chainScore = chainScore;
	}

	/**
	 * Adds a block that will be returned when its height is queried.
	 *
	 * @param block A block to add to the mock cache.
	 */
	public void addBlock(final Block block) {
		this.heightToBlockMap.put(block.getHeight(), block);
	}

	@Override
	public Block getLastBlock() {
		return this.lastBlock;
	}

	@Override
	public BlockChainScore getChainScore() {
		return this.chainScore;
	}

	@Override
	public Block getBlockAt(final BlockHeight height) {
		return this.heightToBlockMap.get(height);
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height) {
		return this.chain;
	}
}

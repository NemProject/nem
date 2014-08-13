package org.nem.nis.test;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.Block;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A mock BlockDao implementation.
 */
public class MockBlockDao implements BlockDao {

	private final HashChain chain;
	private int numFindByIdCalls;
	private int numFindByHashCalls;
	private int numFindByHeightCalls;
	private int numGetHashesFromCalls;

	private long lastFindByIdId;
	private Hash lastFindByHashHash;
	private BlockHeight lastFindByHeightHeight;
	private BlockHeight lastGetHashesFromHeight;
	private int lastGetHashesFromLimit;
	private Long lastId;

	private final ArrayList<Block> blocks;
	private final MockBlockDaoMode mockMode;
	private Block lastSavedBlock;

	/**
	 * Possible mocking modes.
	 */
	public enum MockBlockDaoMode {

		/**
		 * The DAO supports returning a single block from all findBy* methods.
		 */
		SingleBlock,

		/**
		 * The DAO supports multiple blocks and will search through them.
		 */
		MultipleBlocks
	}

	/**
	 * Creates a mock block dao.
	 *
	 * @param block The block to return from findBy* methods.
	 */
	public MockBlockDao(final Block block) {
		this(block, null);
	}

	/**
	 * Creates a mock block dao.
	 *
	 * @param block The block to return from findBy* methods.
	 * @param chain The hash chain to return from getHashesFrom.
	 */
	public MockBlockDao(final Block block, final HashChain chain) {
		this(block, chain, MockBlockDaoMode.SingleBlock);
	}

	/**
	 * Creates a mock block dao.
	 *
	 * @param block The block to return from findBy* methods.
	 * @param chain The hash chain to return from getHashesFrom.
	 * @param mode The mocking mode.
	 */
	public MockBlockDao(final Block block, final HashChain chain, final MockBlockDaoMode mode) {
		this.chain = chain;
		this.blocks = new ArrayList<>();
		this.addBlock(block);
		this.lastId = 1L;
		this.mockMode = mode;
	}

	public void addBlock(final Block block) {
		this.blocks.add(block);
	}

	@Override
	public void save(Block block) {
		if (block.getId() == null) {
			block.setId(this.lastId);
			this.lastId++;
			this.lastSavedBlock = block;
		}
	}

	@Override
	public void updateLastBlockId(Block block) {

	}

	@Override
	public Long count() {
		return null;
	}

	@Override
	public Block findById(long id) {
		++this.numFindByIdCalls;
		this.lastFindByIdId = id;
		return find(block -> block.getId() == id);
	}

	@Override
	public Block findByHash(Hash blockHash) {
		++this.numFindByHashCalls;
		this.lastFindByHashHash = blockHash;
		return find(block -> block.getBlockHash().equals(blockHash));
	}

	@Override
	public Block findByHeight(final BlockHeight height) {
		++this.numFindByHeightCalls;
		this.lastFindByHeightHeight = height;
		return find(block -> block.getHeight() == height.getRaw());
	}

	private Block find(final Predicate<Block> findPredicate) {
		try {
			return MockBlockDaoMode.SingleBlock == this.mockMode
					? this.blocks.get(0)
					: this.blocks.stream().filter(findPredicate).findFirst().get();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height, int limit) {
		++this.numGetHashesFromCalls;
		this.lastGetHashesFromHeight = height;
		this.lastGetHashesFromLimit = limit;
		return this.chain;
	}

	@Override
	public Collection<Block> getBlocksForAccount(final Account account, final Integer timeStamp, int limit) {
		return null;
	}

	@Override
	public List<Block> getBlocksAfter(long blockHeight, int blocksCount) {
		return null;
	}

	@Override
	public List<BlockDifficulty> getDifficultiesFrom(BlockHeight height, int limit) {
		return this.blocks.stream()
				.filter(bl -> bl.getHeight().compareTo(height.getRaw()) > 0)
				.map(bl -> new BlockDifficulty(bl.getDifficulty()))
				.collect(Collectors.toList());
	}

	@Override
	public List<TimeInstant> getTimeStampsFrom(BlockHeight height, int limit) {
		return this.blocks.stream()
				.filter(bl -> bl.getHeight().compareTo(height.getRaw()) > 0)
				.map(bl -> new TimeInstant(bl.getTimeStamp()))
				.collect(Collectors.toList());
	}

	@Override
	public void deleteBlocksAfterHeight(final BlockHeight height) {

	}

	/**
	 * Gets the number of times findById was called.
	 *
	 * @return the number of times findById was called.
	 */
	public int getNumFindByIdCalls() { return this.numFindByIdCalls; }

	/**
	 * Gets the number of times findByHash was called.
	 *
	 * @return the number of times findByHash was called.
	 */
	public int getNumFindByHashCalls() { return this.numFindByHashCalls; }

	/**
	 * Gets the number of times findByHeight was called.
	 *
	 * @return the number of times findByHeight was called.
	 */
	public int getNumFindByHeightCalls() { return this.numFindByHeightCalls; }

	/**
	 * Gets the number of times getHashesFrom was called.
	 *
	 * @return the number of times getHashesFrom was called.
	 */
	public int getNumGetHashesFromCalls() { return this.numGetHashesFromCalls; }

	/**
	 * Gets the last id passed to findById.
	 *
	 * @return The last id passed to findById.
	 */
	public long getLastFindByIdId() { return this.lastFindByIdId; }

	/**
	 * Gets the last hash passed to findByHash.
	 *
	 * @return The last hash passed to findByHash.
	 */
	public Hash getLastFindByHashHash() { return this.lastFindByHashHash; }

	/**
	 * Gets the last height passed to findByHeight.
	 *
	 * @return The last height passed to findByHeight.
	 */
	public BlockHeight getLastFindByHeightHeight() { return this.lastFindByHeightHeight; }

	/**
	 * Gets the last height passed to getHashesFrom.
	 *
	 * @return The last height passed to getHashesFrom.
	 */
	public BlockHeight getLastGetHashesFromHeight() { return this.lastGetHashesFromHeight; }

	/**
	 * Gets the last limit passed to getHashesFrom.
	 *
	 * @return The last limit passed to getHashesFrom.
	 */
	public int getLastGetHashesFromLimit() { return this.lastGetHashesFromLimit; }

	/**
	 * Returns last saved block.
	 *
	 * @return last saved block.
	 */
	public Block getLastSavedBlock() { return lastSavedBlock; }
}
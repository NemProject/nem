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
	private long lastId;

	private final List<Block> blocks;
	private final MockBlockDaoMode mockMode;
	private Block lastSavedBlock;

	// In order to simulate the db we need to update the mock account dao.
	private final MockAccountDao accountDao;

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
	 * @param mode The mocking mode.
	 */
	public MockBlockDao(final MockBlockDaoMode mode, final MockAccountDao accountDao) {
		this.mockMode = mode;
		this.chain = new HashChain(1);
		this.blocks = new ArrayList<>();
		this.accountDao = accountDao;
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
		this.accountDao = new MockAccountDao();
	}

	public void addBlock(final Block block) {
		this.blocks.add(block);
	}

	public Block getLastBlock() {
		return this.blocks.get(this.blocks.size() - 1);
	}

	@Override
	public void save(final Block block) {
		if (null == block.getId()) {
			block.setId(this.lastId);
			this.lastId++;
			this.lastSavedBlock = block;
			this.addBlock(block);
			this.accountDao.blockAdded(block);
		}
	}

	@Override
	public void save(final List<Block> blocks) {
		throw new RuntimeException("unsupported MockBlockDao.save(...List...)");
	}

	@Override
	public Long count() {
		return (long)this.blocks.size();
	}

	@Override
	public Block findByHash(final Hash blockHash) {
		++this.numFindByHashCalls;
		this.lastFindByHashHash = blockHash;
		return this.find(block -> block.getBlockHash().equals(blockHash));
	}

	@Override
	public Block findByHeight(final BlockHeight height) {
		++this.numFindByHeightCalls;
		this.lastFindByHeightHeight = height;
		return this.find(block -> block.getHeight() == height.getRaw());
	}

	private Block find(final Predicate<Block> findPredicate) {
		try {
			return MockBlockDaoMode.SingleBlock == this.mockMode
					? this.blocks.get(0)
					: this.blocks.stream().filter(findPredicate).findFirst().get();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}

	private static Predicate<Block> heightFilter(final BlockHeight height, final int limit) {
		return (block -> block.getHeight().compareTo(height.getRaw()) >= 0 && block.getHeight().compareTo(height.getRaw() + limit) < 0);
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height, final int limit) {
		++this.numGetHashesFromCalls;
		this.lastGetHashesFromHeight = height;
		this.lastGetHashesFromLimit = limit;
		return new HashChain(
				this.blocks.stream()
						.filter(heightFilter(height, limit))
						.map(Block::getBlockHash)
						.collect(Collectors.toList()));
	}

	@Override
	public Collection<Block> getBlocksForAccount(final Account account, final Hash hash, final int limit) {
		return null;
	}

	@Override
	public List<Block> getBlocksAfter(final BlockHeight height, final int limit) {
		return this.blocks.stream()
				.filter(heightFilter(height.next(), limit))
				.collect(Collectors.toList());
	}

	@Override
	public List<BlockDifficulty> getDifficultiesFrom(final BlockHeight height, final int limit) {
		return this.blocks.stream()
				.filter(bl -> bl.getHeight().compareTo(height.getRaw()) >= 0)
				.filter(bl -> bl.getHeight().compareTo(height.getRaw() + limit) < 0)
				.map(bl -> new BlockDifficulty(bl.getDifficulty()))
				.collect(Collectors.toList());
	}

	@Override
	public List<TimeInstant> getTimeStampsFrom(final BlockHeight height, final int limit) {
		return this.blocks.stream()
				.filter(bl -> bl.getHeight().compareTo(height.getRaw()) >= 0)
				.filter(bl -> bl.getHeight().compareTo(height.getRaw() + limit) < 0)
				.map(bl -> new TimeInstant(bl.getTimeStamp()))
				.collect(Collectors.toList());
	}

	@Override
	public void deleteBlocksAfterHeight(final BlockHeight height) {
		final Iterator<Block> iterator = this.blocks.iterator();
		while (iterator.hasNext()) {
			final Block block = iterator.next();
			if (block.getHeight().compareTo(height.getRaw()) > 0) {
				this.accountDao.blockDeleted(block);
				iterator.remove();
				this.lastId--;
			}
		}
	}

	public MockBlockDao shallowCopy() {
		final MockBlockDao copy = new MockBlockDao(this.mockMode, this.accountDao.shallowCopy());
		copy.numFindByIdCalls = this.numFindByIdCalls;
		copy.numFindByHashCalls = this.numFindByHashCalls;
		copy.numFindByHeightCalls = this.numFindByHeightCalls;
		copy.numGetHashesFromCalls = this.numGetHashesFromCalls;
		copy.lastFindByIdId = this.lastFindByIdId;
		copy.lastFindByHashHash = this.lastFindByHashHash;
		copy.lastFindByHeightHeight = this.lastFindByHeightHeight;
		copy.lastGetHashesFromHeight = this.lastGetHashesFromHeight;
		copy.lastGetHashesFromLimit = this.lastGetHashesFromLimit;
		copy.lastId = this.lastId;
		copy.lastSavedBlock = this.lastSavedBlock;
		copy.blocks.addAll(this.blocks);
		this.chain.asCollection().stream().forEach(copy.chain::add);
		return copy;
	}

	// Not exactly what equals should look like but good enough for us.
	public boolean equals(final MockBlockDao rhs) {
		if (this.blocks.size() != rhs.blocks.size()) {
			return false;
		}

		for (int i = 0; i < this.blocks.size(); i++) {
			if (!this.blocks.get(i).getBlockHash().equals(rhs.blocks.get(i).getBlockHash())) {
				return false;
			}
		}

		if (!this.accountDao.equals(rhs.accountDao)) {
			return false;
		}

		return this.mockMode == rhs.mockMode &&
				this.lastId == rhs.lastId &&
				this.lastSavedBlock.getBlockHash().equals(rhs.lastSavedBlock.getBlockHash()) &&
				this.chain.asCollection().equals(rhs.chain.asCollection());
	}

	public MockAccountDao getAccountDao() {
		return this.accountDao;
	}

	/**
	 * Gets the number of times findById was called.
	 *
	 * @return the number of times findById was called.
	 */
	public int getNumFindByIdCalls() {
		return this.numFindByIdCalls;
	}

	/**
	 * Gets the number of times findByHash was called.
	 *
	 * @return the number of times findByHash was called.
	 */
	public int getNumFindByHashCalls() {
		return this.numFindByHashCalls;
	}

	/**
	 * Gets the number of times findByHeight was called.
	 *
	 * @return the number of times findByHeight was called.
	 */
	public int getNumFindByHeightCalls() {
		return this.numFindByHeightCalls;
	}

	/**
	 * Gets the number of times getHashesFrom was called.
	 *
	 * @return the number of times getHashesFrom was called.
	 */
	public int getNumGetHashesFromCalls() {
		return this.numGetHashesFromCalls;
	}

	/**
	 * Gets the last id passed to findById.
	 *
	 * @return The last id passed to findById.
	 */
	public long getLastFindByIdId() {
		return this.lastFindByIdId;
	}

	/**
	 * Gets the last hash passed to findByHash.
	 *
	 * @return The last hash passed to findByHash.
	 */
	public Hash getLastFindByHashHash() {
		return this.lastFindByHashHash;
	}

	/**
	 * Gets the last height passed to findByHeight.
	 *
	 * @return The last height passed to findByHeight.
	 */
	public BlockHeight getLastFindByHeightHeight() {
		return this.lastFindByHeightHeight;
	}

	/**
	 * Gets the last height passed to getHashesFrom.
	 *
	 * @return The last height passed to getHashesFrom.
	 */
	public BlockHeight getLastGetHashesFromHeight() {
		return this.lastGetHashesFromHeight;
	}

	/**
	 * Gets the last limit passed to getHashesFrom.
	 *
	 * @return The last limit passed to getHashesFrom.
	 */
	public int getLastGetHashesFromLimit() {
		return this.lastGetHashesFromLimit;
	}

	/**
	 * Returns last saved block.
	 *
	 * @return last saved block.
	 */
	public Block getLastSavedBlock() {
		return this.lastSavedBlock;
	}
}
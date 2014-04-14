package org.nem.nis.test;

import org.nem.core.model.Hash;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.Block;

import java.util.List;

/**
 * A mock BlockDao implementation.
 */
public class MockBlockDao implements BlockDao {

	private final Block block;
	private final List<byte[]> hashes;
	private int numFindByIdCalls;
	private int numFindByHashCalls;
	private int numFindByHeightCalls;
	private int numGetHashesFromCalls;

	private long lastFindByIdId;
	private Hash lastFindByHashHash;
	private long lastFindByHeightHeight;
	private long lastGetHashesFromHeight;
	private int lastGetHashesFromLimit;

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
	 * @param hashes The hashes to return from getHashesFrom.
	 */
	public MockBlockDao(final Block block, final List<byte[]> hashes) {
		this.block = block;
		this.hashes = hashes;
	}


	@Override
	public void save(Block block) {

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
		return this.block;
	}

	@Override
	public Block findByHash(Hash blockHash) {
		++this.numFindByHashCalls;
		this.lastFindByHashHash = blockHash;
		return this.block;
	}

	@Override
	public Block findByHeight(long blockHeight) {
		++this.numFindByHeightCalls;
		this.lastFindByHeightHeight = blockHeight;
		return this.block;
	}

	@Override
	public List<byte[]> getHashesFrom(long blockHeight, int limit) {
		++this.numGetHashesFromCalls;
		this.lastGetHashesFromHeight = blockHeight;
		this.lastGetHashesFromLimit = limit;
		return this.hashes;
	}

	@Override
	public void deleteBlocksAfterHeight(long height) {

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
	public long getLastFindByHeightHeight() { return this.lastFindByHeightHeight; }

	/**
	 * Gets the last height passed to getHashesFrom.
	 *
	 * @return The last height passed to getHashesFrom.
	 */
	public long getLastGetHashesFromHeight() { return this.lastGetHashesFromHeight; }

	/**
	 * Gets the last limit passed to getHashesFrom.
	 *
	 * @return The last limit passed to getHashesFrom.
	 */
	public int getLastGetHashesFromLimit() { return this.lastGetHashesFromLimit; }
}
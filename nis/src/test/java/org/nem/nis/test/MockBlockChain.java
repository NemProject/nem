package org.nem.nis.test;

import org.nem.nis.BlockChain;
import org.nem.nis.dbmodel.Block;

/**
 * A mock BlockChain implementation.
 */
public class MockBlockChain extends BlockChain {

	private final Block lastDbBlock;
	private int numGetLastDbBlockCalls;

	/**
	 * Creates a new mock block chain.
	 */
	public MockBlockChain() {
		this(new Block());
	}

	/**
	 * Creates a new mock block chain.
	 *
	 * @param lastBlock The last block.
	 */
	public MockBlockChain(final Block lastBlock) {
		super(null, null, null, null, null);
		this.lastDbBlock = lastBlock;
	}

	/**
	 * Gets the number of times getLastDbBlock was called.
	 *
	 * @return The number of times getLastDbBlock was called.
	 */
	public int getNumGetLastDbBlockCalls() {
		return this.numGetLastDbBlockCalls;
	}
}

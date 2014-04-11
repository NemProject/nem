package org.nem.nis.sync;

public class SynchronizeContext {
	public long commonBlockHeight;
	public boolean hasOwnChain;

	public SynchronizeContext(long commonBlockHeight, boolean hasOwnChain) {
		this.commonBlockHeight = commonBlockHeight;
		this.hasOwnChain = hasOwnChain;
	}
}

package org.nem.nis.test.BlockChain;

public class TestOptions {
	private final int numAccounts;
	private final int numNodes;
	private final int commonChainHeight;

	public TestOptions(final int numAccounts, final int numNodes, final int commonChainHeight) {
		this.numAccounts = numAccounts;
		this.numNodes = numNodes;
		this.commonChainHeight = commonChainHeight;
	}

	public int numAccounts() {
		return this.numAccounts;
	}

	public int numNodes() {
		return this.numNodes;
	}

	public int commonChainHeight() {
		return this.commonChainHeight;
	}
}

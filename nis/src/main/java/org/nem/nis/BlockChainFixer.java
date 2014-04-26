package org.nem.nis;

import org.nem.core.model.Block;
import org.nem.core.model.HashUtils;

import java.util.Collection;

/**
 * Helper class for Validator, that fixes elements of chain before validation
 */
public class BlockChainFixer {
	/**
	 * Calculate and fix generation hashes for chains of blocks.
	 *
	 * @param parentBlock parent block of a chain.
	 * @param peerChain chain to fix.
	 */
	public static void calculatePeerChainGenerations(Block parentBlock, final Collection<Block> peerChain) {
		for (Block block : peerChain) {
			block.setGenerationHash(HashUtils.nextHash(parentBlock.getGenerationHash(), block.getSigner().getKeyPair().getPublicKey()));

			parentBlock = block;
		}
	}

	/**
	 * Calculate and fix parent hashes for chain of blocks.
	 *
	 * @param parentBlock parent block of a chain.
	 * @param peerChain chain to fix.
	 */
	public static void calculateChainParents(Block parentBlock, final Collection<Block> peerChain) {
		for (Block block : peerChain) {
			block.setPreviousBlockHash(HashUtils.calculateHash(parentBlock));

			parentBlock = block;
		}
	}
}

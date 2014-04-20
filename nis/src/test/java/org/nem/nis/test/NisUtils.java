package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Transfer;

import java.util.ArrayList;
import java.util.List;

/**
 * Static class containing NIS test helper functions.
 */
public class NisUtils {

	/**
	 * Creates a DB Block that can be mapped to a model Block.
	 *
	 * @param timeStamp The block timestamp.
	 * @return The db block.
	 */
	public static org.nem.nis.dbmodel.Block createBlockWithTimeStamp(final int timeStamp) {
		final org.nem.nis.dbmodel.Account account = new org.nem.nis.dbmodel.Account();
		account.setPublicKey(Utils.generateRandomPublicKey());

		final org.nem.nis.dbmodel.Block block = new org.nem.nis.dbmodel.Block();
		block.setForgerId(account);
		block.setTimestamp(timeStamp);
		block.setHeight(10L);
		block.setForgerProof(Utils.generateRandomBytes(64));
		block.setBlockTransfers(new ArrayList<Transfer>());
		return block;
	}

	/**
	 * Creates a new random Block.
	 */
	public static Block createRandomBlock() {
		return new Block(
				Utils.generateRandomAccount(),
				new Hash(Utils.generateRandomBytes()),
				TimeInstant.ZERO,
				BlockHeight.ONE);
	}

	/**
	 * Creates a new random Block with the specified height.
	 */
	public static Block createRandomBlock(long height) {
		return new Block(
				Utils.generateRandomAccount(),
				new Hash(Utils.generateRandomBytes()),
				TimeInstant.ZERO,
				new BlockHeight(height));
	}

	/**
	 * Creates a raw hashes list.
	 *
	 * @param numHashes The number of hashes desired.
	 * @return A raw hashes list.
	 */
	public static List<byte[]> createRawHashesList(final int numHashes) {
		final List<byte[]> hashes = new ArrayList<>();
		for (int i = 0; i < numHashes; ++i)
			hashes.add(Utils.generateRandomBytes(64));

		return hashes;
	}
}

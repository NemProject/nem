package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.dao.*;
import org.nem.nis.secret.AccountLink;
import org.nem.nis.validators.*;

import java.util.*;

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
	public static org.nem.nis.dbmodel.Block createDbBlockWithTimeStamp(final int timeStamp) {
		final org.nem.nis.dbmodel.Account account = new org.nem.nis.dbmodel.Account();
		account.setPublicKey(Utils.generateRandomPublicKey());

		final org.nem.nis.dbmodel.Block block = new org.nem.nis.dbmodel.Block();
		block.setForger(account);
		block.setTimeStamp(timeStamp);
		block.setHeight(10L);
		block.setForgerProof(Utils.generateRandomBytes(64));
		block.setBlockTransfers(new ArrayList<>());
		return block;
	}

	/**
	 * Creates a new random Block.
	 *
	 * @return The block.
	 */
	public static Block createRandomBlock() {
		return new Block(
				Utils.generateRandomAccount(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				TimeInstant.ZERO,
				BlockHeight.ONE);
	}

	/**
	 * Creates a new random Block with the specified height.
	 *
	 * @param height The height.
	 * @return The block.
	 */
	public static Block createRandomBlockWithHeight(final long height) {
		return createRandomBlockWithHeight(Utils.generateRandomAccount(), height);
	}

	/**
	 * Creates a new random Block with the specified height and signer
	 *
	 * @param signer The signer.
	 * @param height The height.
	 * @return The block.
	 */
	public static Block createRandomBlockWithHeight(final Account signer, final long height) {
		return new Block(
				signer,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				TimeInstant.ZERO,
				new BlockHeight(height));
	}

	/**
	 * Creates a new random Block with the specified timestamp.
	 *
	 * @param timeStamp The time stamp.
	 * @return The block.
	 */
	public static Block createRandomBlockWithTimeStamp(final int timeStamp) {
		return new Block(
				Utils.generateRandomAccount(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				new TimeInstant(timeStamp),
				BlockHeight.ONE);
	}

	/**
	 * Creates a new list of blocks.
	 *
	 * @param parent The parent block.
	 * @param numBlocks The number of blocks.
	 * @return The block list.
	 */
	public static List<Block> createBlockList(Block parent, final int numBlocks) {
		final List<Block> blocks = new ArrayList<>();
		final Account account = Utils.generateRandomAccount();
		for (int i = 0; i < numBlocks; ++i) {
			final Block block = new Block(account, parent, TimeInstant.ZERO);
			blocks.add(block);
			parent = block;
		}

		signAllBlocks(blocks);
		return blocks;
	}

	/**
	 * Signs all blocks.
	 *
	 * @param blocks The blocks to sign.
	 */
	public static void signAllBlocks(final List<Block> blocks) {
		for (final Block block : blocks) {
			block.sign();
		}
	}

	/**
	 * Creates a hashes list.
	 *
	 * @param numHashes The number of hashes desired.
	 * @return A hashes list.
	 */
	public static List<Hash> createHashesList(final int numHashes) {
		final List<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < numHashes; ++i) {
			hashes.add(Utils.generateRandomHash());
		}

		return hashes;
	}

	/**
	 * Creates a new account link.
	 *
	 * @param blockHeight The block height.
	 * @param amount The amount.
	 * @param address The address.
	 * @return The account link.
	 */
	public static AccountLink createLink(final int blockHeight, final long amount, final String address) {
		return new AccountLink(
				new BlockHeight(blockHeight),
				Amount.fromNem(amount),
				Address.fromEncoded(address));
	}

	/**
	 * Creates a transaction validator factory.
	 *
	 * @return The factory.
	 */
	public static TransactionValidatorFactory createTransactionValidatorFactory() {
		return createTransactionValidatorFactory(Mockito.mock(TransferDao.class));
	}

	/**
	 * Creates a transaction validator factory.
	 *
	 * @param transferDao The transfer dao.
	 * @return The factory.
	 */
	public static TransactionValidatorFactory createTransactionValidatorFactory(final TransferDao transferDao) {
		return new TransactionValidatorFactory(
				transferDao,
				Mockito.mock(ImportanceTransferDao.class),
				new SystemTimeProvider());
	}

	/**
	 * Creates a block validator factory.
	 *
	 * @return The factory.
	 */
	public static BlockValidatorFactory createBlockValidatorFactory() {
		return new BlockValidatorFactory(new SystemTimeProvider());
	}

	/**
	 * Converts an array of integers to an array of node ids.
	 *
	 * @param ids The integer array.
	 * @return The node id array.
	 */
	public static NodeId[] toNodeIdArray(final int... ids) {
		final NodeId[] nodeIds = new NodeId[ids.length];
		for (int i = 0; i < ids.length; ++i) {
			nodeIds[i] = new NodeId(ids[i]);
		}

		return nodeIds;
	}

	/**
	 * Converts an array of integers to a list of node ids.
	 *
	 * @param ids The integer array.
	 * @return The node id list.
	 */
	public static List<NodeId> toNodeIdList(final int... ids) {
		return Arrays.asList(NisUtils.toNodeIdArray(ids));
	}
}

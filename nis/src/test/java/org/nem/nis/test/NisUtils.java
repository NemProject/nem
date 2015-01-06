package org.nem.nis.test;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;
import org.nem.nis.poi.*;
import org.nem.nis.poi.graph.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.AccountLink;
import org.nem.nis.validators.*;

import java.util.*;

/**
 * Static class containing NIS test helper functions.
 */
public class NisUtils {
	private static final PoiOptions DEFAULT_POI_OPTIONS = new PoiOptionsBuilder().create();

	/**
	 * Creates a dummy DbBlock.
	 *
	 * @param harvester The harvester account.
	 * @return The db block.
	 */
	public static DbBlock createDummyDbBlock(final DbAccount harvester) {
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setBlockHash(Hash.ZERO);
		dbBlock.setVersion(1);
		dbBlock.setGenerationHash(Hash.ZERO);
		dbBlock.setPrevBlockHash(Hash.ZERO);
		dbBlock.setTimeStamp(TimeInstant.ZERO.getRawTime());
		dbBlock.setHarvester(harvester);
		dbBlock.setHarvesterProof(Utils.generateRandomSignature().getBytes());
		dbBlock.setHeight(1L);
		dbBlock.setTotalFee(1L);
		dbBlock.setDifficulty(1L);
		return dbBlock;
	}

	/**
	 * Creates a DbBlock that can be mapped to a model Block.
	 *
	 * @param timeStamp The block timestamp.
	 * @return The db block.
	 */
	public static DbBlock createDbBlockWithTimeStamp(final int timeStamp) {
		return createDbBlockWithTimeStampAtHeight(timeStamp, 10);
	}

	/**
	 * Creates a DbBlock that can be mapped to a model Block.
	 *
	 * @param timeStamp The block timestamp.
	 * @param height The block height.
	 * @return The db block.
	 */
	public static DbBlock createDbBlockWithTimeStampAtHeight(final int timeStamp, final long height) {
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final DbAccount account = new DbAccount();
		account.setPrintableKey(address.getEncoded());
		account.setPublicKey(address.getPublicKey());

		final DbBlock block = new DbBlock();
		block.setHarvester(account);
		block.setTimeStamp(timeStamp);
		block.setHeight(height);
		block.setHarvesterProof(Utils.generateRandomBytes(64));
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			entry.setInBlock.accept(block, new ArrayList<>());
		}

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
	 * Creates a new random Block with the specified height and signer.
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
	 * Creates a new random Block with the specified timestamp and height.
	 *
	 * @param timeStamp The time stamp.
	 * @param height The height.
	 * @return The block.
	 */
	public static Block createRandomBlockWithTimeStampAndHeight(final int timeStamp, final long height) {
		return new Block(
				Utils.generateRandomAccount(),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				new TimeInstant(timeStamp),
				new BlockHeight(height));
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
	 * Creates a new list of blocks that are 10 minutes apart.
	 *
	 * @param signer The block signer.
	 * @param parent The parent block.
	 * @param numBlocks The number of blocks.
	 * @param timeStamp The timestamp ten minutes before the first block.
	 * @return The block list.
	 */
	public static List<Block> createBlockList(final Account signer, Block parent, final int numBlocks, final TimeInstant timeStamp) {
		final List<Block> blocks = new ArrayList<>();
		for (int i = 0; i < numBlocks; ++i) {
			// 10 minutes between blocks
			final Block block = new Block(signer, parent, timeStamp.addMinutes((i + 1) * 10));
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
	 * Creates a (mostly real) transaction validator factory.
	 *
	 * @return The factory.
	 */
	public static TransactionValidatorFactory createTransactionValidatorFactory() {
		return new TransactionValidatorFactory(
				new SystemTimeProvider(),
				DEFAULT_POI_OPTIONS);
	}

	/**
	 * Creates a (real) block validator factory.
	 *
	 * @return The factory.
	 */
	public static BlockValidatorFactory createBlockValidatorFactory() {
		return new BlockValidatorFactory(new SystemTimeProvider());
	}

	/**
	 * Creates a (real) importance calculator
	 *
	 * @return The calculator.
	 */
	public static ImportanceCalculator createImportanceCalculator() {
		return new PoiImportanceCalculator(new PoiScorer(), DEFAULT_POI_OPTIONS);
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

	/**
	 * Converts an array of integers into a NodeNeighbors object.
	 *
	 * @param ids The integer array.
	 * @return The node neighbors.
	 */
	public static NodeNeighbors createNeighbors(final int... ids) {
		return new NodeNeighbors(NisUtils.toNodeIdArray(ids));
	}

	/**
	 * Creates a new neighborhood with default clustering parameters.
	 *
	 * @param repository The neighborhood repository.
	 * @param similarityStrategy The similarity strategy.
	 * @return The neighborhood.
	 */
	public static Neighborhood createNeighborhood(
			final NeighborhoodRepository repository,
			final SimilarityStrategy similarityStrategy) {
		return new Neighborhood(
				repository,
				similarityStrategy,
				DEFAULT_POI_OPTIONS.getMuClusteringValue(),
				DEFAULT_POI_OPTIONS.getEpsilonClusteringValue());
	}

	//region createBlockNotificationContext

	/**
	 * Creates a block notification context.
	 *
	 * @return The block notification context.
	 */
	public static BlockNotificationContext createBlockNotificationContext() {
		return createBlockNotificationContext(NotificationTrigger.Execute);
	}

	/**
	 * Creates a block notification context.
	 *
	 * @param trigger The notification trigger.
	 * @return The block notification context.
	 */
	public static BlockNotificationContext createBlockNotificationContext(final NotificationTrigger trigger) {
		return createBlockNotificationContext(new BlockHeight(8888), trigger);
	}

	/**
	 * Creates a block notification context.
	 *
	 * @param height The notification height.
	 * @param trigger The notification trigger.
	 * @return The block notification context.
	 */
	public static BlockNotificationContext createBlockNotificationContext(final BlockHeight height, final NotificationTrigger trigger) {
		return new BlockNotificationContext(height, new TimeInstant(987), trigger);
	}

	//endregion
}

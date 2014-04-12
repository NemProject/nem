package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.MockBlockScorer;

import java.security.InvalidParameterException;

public class BlockChainComparerTest {

	//region last block comparison

	@Test(expected = InvalidParameterException.class)
	public void localBlockChainMustHaveAtLeastOneBlock() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		comparer.compare(
				new MockBlockLookup(null),
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 7)));
	}

	@Test
	public void remoteHasNoBlocksIfRemoteDoesNotHaveLastBlock() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 7)),
				new MockBlockLookup(null)).getCode();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_HAS_NO_BLOCKS));
	}

	@Test
	public void remoteHasNonVerifiableBlocksIfRemoteLastBlockDoesNotVerify() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 7)),
				new MockBlockLookup(createNonVerifiableBlock(Utils.generateRandomAccount(), 7))).getCode();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_HAS_NON_VERIFIABLE_BLOCK));
	}

	@Test
	public void remoteIsSyncedIfLocalAndRemoteChainsHaveSameLastBlock() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(account, 7)),
				new MockBlockLookup(createVerifiableBlock(account, 7))).getCode();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_SYNCED));
	}

	@Test
	public void remoteIsTooFarBehindIfRemoteIsMoreThanMaxBlocksToRewriteBehindLocal() {
		// Assert:
		Assert.assertThat(
				compareBlocksWithHeight(18, 7),
				IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND));
	}

	@Test
	public void remoteIsNotTooFarBehindIfRemoteIsMaxBlocksToRewriteBehindLocal() {
		// Assert:
		Assert.assertThat(
				compareBlocksWithHeight(17, 7),
				IsNot.not(IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND)));
	}

	@Test
	public void localCanBeMoreThanMaxBlocksToRewriteBehindLocal() {
		// Assert:
		Assert.assertThat(
				compareBlocksWithHeight(7, 18),
				IsNot.not(IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND)));
	}

	private static int compareBlocksWithHeight(int localHeight, int remoteHeight) {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		return comparer.compare(
				new MockBlockLookup(createVerifiableBlock(account, localHeight)),
				new MockBlockLookup(createVerifiableBlock(account, remoteHeight))).getCode();
	}

	//endregion

	//region chain comparison

	@Test
	public void remoteReturnedTooManyHashesIfItReturnedMoreThanMaxBlocksTAnalyze() {
		// Assert:
		Assert.assertThat(
				compareBlocksWithNumRemoteHashes(21),
				IsEqual.equalTo(ComparisonResult.Code.REMOTE_RETURNED_TOO_MANY_HASHES));
	}

	@Test
	public void remoteDidNotReturnTooManyHashesIfItReturnedExactlyMaxBlocksTAnalyze() {
		// Assert:
		Assert.assertThat(
				compareBlocksWithNumRemoteHashes(20),
				IsNot.not(IsEqual.equalTo(ComparisonResult.Code.REMOTE_RETURNED_TOO_MANY_HASHES)));
	}

	private static int compareBlocksWithNumRemoteHashes(int numHashes) {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		return comparer.compare(
				new MockBlockLookup(createVerifiableBlock(account, 7)),
				new MockBlockLookup(createVerifiableBlock(account, 8), numHashes)).getCode();
	}

	@Test
	public void remoteReturnedInvalidHashesIfTheFirstLocalAndRemoteHashesDoNotMatch() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(account, 7), 1),
				new MockBlockLookup(createVerifiableBlock(account, 8), 2)).getCode();

		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_RETURNED_INVALID_HASHES));
	}

	@Test
	public void remoteIsSyncedIfLocalIsSameSizeAsRemoteChainAndContainsAllHashesInRemoteChain() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		final byte[][] commonHashes = new byte[][] { Utils.generateRandomBytes(32), Utils.generateRandomBytes(32) };
		final HashChain localChain = createHashChain(commonHashes);
		final HashChain remoteChain = createHashChain(commonHashes);

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), localChain),
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), remoteChain)).getCode();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_SYNCED));
	}

	@Test
	public void remoteIsSyncedIfLocalIsLargerThanRemoteChainAndContainsAllHashesInRemoteChain() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		final byte[][] commonHashes = new byte[][] { Utils.generateRandomBytes(32), Utils.generateRandomBytes(32) };
		final HashChain localChain = createHashChain(commonHashes);
		localChain.add(Utils.generateRandomBytes(32));
		final HashChain remoteChain = createHashChain(commonHashes);

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), localChain),
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), remoteChain)).getCode();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_SYNCED));
	}

	@Test
	public void remoteIsNotSyncedIfLocalIsSmallerThanRemoteChainAndContainsAllHashesInRemoteChain() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer(5);

		final byte[][] commonHashes = new byte[][] { Utils.generateRandomBytes(32), Utils.generateRandomBytes(32) };
		final HashChain localChain = createHashChain(commonHashes);
		final HashChain remoteChain = createHashChain(commonHashes);
		remoteChain.add(Utils.generateRandomBytes(32));

		// Act:
		final ComparisonResult result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), localChain),
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), remoteChain));

		// Assert:
		Assert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		Assert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(4L));
		Assert.assertThat(result.areChainsConsistent(), IsEqual.equalTo(true));
	}

	@Test
	public void remoteIsNotSyncedIfLocalIsSameSizeAsRemoteChainButContainsAtLeastOneDifferentHash() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer(5);

		final byte[][] commonHashes = new byte[][] { Utils.generateRandomBytes(32), Utils.generateRandomBytes(32) };
		final HashChain localChain = createHashChain(commonHashes);
		localChain.add(Utils.generateRandomBytes(32));
		final HashChain remoteChain = createHashChain(commonHashes);
		remoteChain.add(Utils.generateRandomBytes(32));

		// Act:
		final ComparisonResult result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), localChain),
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8), remoteChain));

		// Assert:
		Assert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		Assert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(4L));
		Assert.assertThat(result.areChainsConsistent(), IsEqual.equalTo(false));
	}

	//endregion

	private static HashChain createHashChain(byte[]... hashes) {
		final HashChain chain = new HashChain(hashes.length);
		for (final byte[] hash : hashes)
			chain.add(hash);

		return chain;
	}

	private static Block createVerifiableBlock(final Account account, final long height) {
		final Block block = new Block(account, Hash.ZERO, TimeInstant.ZERO, height);
		block.sign();
		return block;
	}

	private static Block createNonVerifiableBlock(final Account account, final long height) {
		final Block block = new Block(account, Hash.ZERO, TimeInstant.ZERO, height);
		block.setSignature(new Signature(Utils.generateRandomBytes(64)));
		return block;
	}

	private static BlockChainComparer createBlockChainComparer(final int maxNumBlocksToRewrite) {
		final ComparisonContext context = new ComparisonContext(20, maxNumBlocksToRewrite, new MockBlockScorer());
		return new BlockChainComparer(context);
	}

	private static BlockChainComparer createBlockChainComparer() {
		return createBlockChainComparer(10);
	}

	private static class MockBlockLookup implements BlockLookup {

		private final Block lastBlock;
		private final HashChain chain;

		public MockBlockLookup(final Block lastBlock) {
			this(lastBlock, 1);
		}

		public MockBlockLookup(final Block lastBlock, int numHashesToReturn) {
			this.lastBlock = lastBlock;

			this.chain = new HashChain(numHashesToReturn);
			for (int i = 0; i < numHashesToReturn; ++i)
				this.chain.add(Utils.generateRandomBytes(64));
		}

		public MockBlockLookup(final Block lastBlock, final HashChain hashChain) {
			this.lastBlock = lastBlock;
			this.chain = hashChain;
		}

		@Override
		public Block getLastBlock() {
			return this.lastBlock;
		}

		@Override
		public Block getBlockAt(long height) {
			return null;
		}

		@Override
		public HashChain getHashesFrom(long height) {
			return this.chain;
		}
	}
}
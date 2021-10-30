package org.nem.nis.sync;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.MockBlockLookup;

public class BlockChainComparerTest {

	// region chain score is compared

	@Test
	public void remoteReportedLowerOrEqualChainScoreIfRemoteChainScoreIsLessThanLocalChainScore() {
		// Assert:
		MatcherAssert.assertThat(compareDifferentChainScores(10, 9),
				IsEqual.equalTo(ComparisonResult.Code.REMOTE_REPORTED_LOWER_CHAIN_SCORE));
	}

	@Test
	public void remoteReportedLowerOrEqualChainScoreIfRemoteChainScoreIsEqualToLocalChainScore() {
		// Assert:
		MatcherAssert.assertThat(compareDifferentChainScores(10, 10),
				IsEqual.equalTo(ComparisonResult.Code.REMOTE_REPORTED_EQUAL_CHAIN_SCORE));
	}

	@Test
	public void chainScoreCheckPassesIfRemoteChainScoreIsGreaterThanLocalChainScore() {
		// Assert:
		MatcherAssert.assertThat(compareDifferentChainScores(10, 11), IsEqual.equalTo(ComparisonResult.Code.REMOTE_HAS_NO_BLOCKS));
	}

	private static ComparisonResult.Code compareDifferentChainScores(final int localChainScore, final int remoteChainScore) {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		return comparer.compare(new MockBlockLookup(createVerifiableBlock(7), new BlockChainScore(localChainScore)),
				new MockBlockLookup(null, new BlockChainScore(remoteChainScore))).getCode();
	}

	// endregion

	// region last block comparison

	@Test(expected = IllegalArgumentException.class)
	public void localBlockChainMustHaveAtLeastOneBlock() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		comparer.compare(new MockBlockLookup(null), new MockBlockLookup(createVerifiableBlock(7)));
	}

	@Test
	public void remoteChainHeightIsStoredInResult() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		final BlockHeight height = comparer.compare(new MockBlockLookup(createVerifiableBlock(account, 27)),
				new MockBlockLookup(createVerifiableBlock(account, 11), new BlockChainScore(10))).getRemoteHeight();

		// Assert:
		MatcherAssert.assertThat(height, IsEqual.equalTo(new BlockHeight(11)));
	}

	@Test
	public void remoteHasNoBlocksIfRemoteDoesNotHaveLastBlock() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		final ComparisonResult.Code result = comparer
				.compare(new MockBlockLookup(createVerifiableBlock(7)), new MockBlockLookup(null, new BlockChainScore(10))).getCode();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_HAS_NO_BLOCKS));
	}

	@Test
	public void remoteHasNonVerifiableBlocksIfRemoteLastBlockDoesNotVerify() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		final ComparisonResult.Code result = comparer.compare(new MockBlockLookup(createVerifiableBlock(7)),
				new MockBlockLookup(createNonVerifiableBlock(7), new BlockChainScore(10))).getCode();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_HAS_NON_VERIFIABLE_BLOCK));
	}

	@Test
	public void remoteIsSyncedIfLocalAndRemoteChainsHaveSameLastBlock() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		final ComparisonResult.Code result = comparer.compare(new MockBlockLookup(createVerifiableBlock(account, 7)),
				new MockBlockLookup(createVerifiableBlock(account, 7), new BlockChainScore(10))).getCode();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_SYNCED));
	}

	@Test
	public void remoteIsTooFarBehindIfRemoteIsMoreThanMaxBlocksToRewriteBehindLocal() {
		// Assert:
		MatcherAssert.assertThat(compareBlocksWithHeight(18, 7), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND));
	}

	@Test
	public void remoteIsNotTooFarBehindIfRemoteIsMaxBlocksToRewriteBehindLocal() {
		// Assert:
		MatcherAssert.assertThat(compareBlocksWithHeight(17, 7),
				IsNot.not(IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND)));
	}

	@Test
	public void localCanBeMoreThanMaxBlocksToRewriteBehindLocal() {
		// Assert:
		MatcherAssert.assertThat(compareBlocksWithHeight(7, 18),
				IsNot.not(IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND)));
	}

	private static ComparisonResult.Code compareBlocksWithHeight(final int localHeight, final int remoteHeight) {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		return comparer.compare(new MockBlockLookup(createVerifiableBlock(account, localHeight)),
				new MockBlockLookup(createVerifiableBlock(account, remoteHeight), new BlockChainScore(10))).getCode();
	}

	// endregion

	// region chain comparison

	@Test
	public void remoteReturnedTooManyHashesIfItReturnedMoreThanMaxBlocksToAnalyze() {
		// Assert:
		MatcherAssert.assertThat(compareBlocksWithNumRemoteHashes(21),
				IsEqual.equalTo(ComparisonResult.Code.REMOTE_RETURNED_TOO_MANY_HASHES));
	}

	@Test
	public void remoteDidNotReturnTooManyHashesIfItReturnedExactlyMaxBlocksToAnalyze() {
		// Assert:
		MatcherAssert.assertThat(compareBlocksWithNumRemoteHashes(20),
				IsNot.not(IsEqual.equalTo(ComparisonResult.Code.REMOTE_RETURNED_TOO_MANY_HASHES)));
	}

	private static ComparisonResult.Code compareBlocksWithNumRemoteHashes(final int numHashes) {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		return comparer.compare(new MockBlockLookup(createVerifiableBlock(account, 7)),
				new MockBlockLookup(createVerifiableBlock(account, 8), new BlockChainScore(10), numHashes)).getCode();
	}

	@Test
	public void remoteReturnedInvalidHashesIfTheFirstLocalAndRemoteHashesDoNotMatch() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		final ComparisonResult.Code result = comparer.compare(new MockBlockLookup(createVerifiableBlock(account, 7), 1),
				new MockBlockLookup(createVerifiableBlock(account, 8), new BlockChainScore(10), 2)).getCode();

		MatcherAssert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_RETURNED_INVALID_HASHES));
	}

	@Test
	public void remoteLiedAboutChainScoreIfLocalIsSameSizeAsRemoteChainAndContainsAllHashesInRemoteChain() {
		// hashes are only compared if the remote promises a better chain
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		final byte[][] commonHashes = new byte[][]{
				Utils.generateRandomBytes(32), Utils.generateRandomBytes(32)
		};
		final HashChain localChain = createHashChain(commonHashes);
		final HashChain remoteChain = createHashChain(commonHashes);

		// Act:
		final ComparisonResult.Code result = comparer.compare(new MockBlockLookup(createVerifiableBlock(8), localChain),
				new MockBlockLookup(createVerifiableBlock(8), new BlockChainScore(10), remoteChain)).getCode();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_LIED_ABOUT_CHAIN_SCORE));
	}

	@Test
	public void remoteLiedAboutChainScoreIfRemoteChainIsSubsetOfLocalChainButRemoteReportedHigherScore() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		final byte[][] commonHashes = new byte[][]{
				Utils.generateRandomBytes(32), Utils.generateRandomBytes(32)
		};
		final HashChain localChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));
		final HashChain remoteChain = createHashChain(commonHashes);

		// Act:
		final ComparisonResult.Code result = comparer.compare(new MockBlockLookup(createVerifiableBlock(8), localChain),
				new MockBlockLookup(createVerifiableBlock(8), new BlockChainScore(10), remoteChain)).getCode();

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ComparisonResult.Code.REMOTE_LIED_ABOUT_CHAIN_SCORE));
	}

	@Test
	public void remoteIsNotSyncedIfLocalIsSmallerThanRemoteChainAndContainsAllHashesInRemoteChain() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer(5);

		final byte[][] commonHashes = new byte[][]{
				Utils.generateRandomBytes(32), Utils.generateRandomBytes(32)
		};
		final HashChain localChain = createHashChain(commonHashes);
		final HashChain remoteChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));

		// Act:
		final ComparisonResult result = comparer.compare(new MockBlockLookup(createVerifiableBlock(8), localChain),
				new MockBlockLookup(createVerifiableBlock(8), new BlockChainScore(10), remoteChain));

		// Assert:
		MatcherAssert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		MatcherAssert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(4L));
		MatcherAssert.assertThat(result.areChainsConsistent(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(result.getRemoteHeight(), IsEqual.equalTo(new BlockHeight(8)));
	}

	@Test
	public void remoteHasNonVerifiableBlockIfFirstDifferentRemoteBlockIsNotVerifiable() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer(5);

		final byte[][] commonHashes = new byte[][]{
				Utils.generateRandomBytes(32), Utils.generateRandomBytes(32)
		};
		final HashChain localChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));
		final HashChain remoteChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));

		final MockBlockLookup remoteBlockLookup = new MockBlockLookup(createVerifiableBlock(8), new BlockChainScore(10), remoteChain);
		remoteBlockLookup.addBlock(createNonVerifiableBlock(5));

		// Act:
		final ComparisonResult result = comparer.compare(new MockBlockLookup(createVerifiableBlock(8), localChain), remoteBlockLookup);

		// Assert:
		MatcherAssert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_HAS_NON_VERIFIABLE_BLOCK));
	}

	@Test
	public void remoteIsNotSyncedIfFirstDifferentRemoteBlockIsVerifiable() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer(5);

		final byte[][] commonHashes = new byte[][]{
				Utils.generateRandomBytes(32), Utils.generateRandomBytes(32)
		};
		final HashChain localChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));
		final HashChain remoteChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));

		final MockBlockLookup remoteBlockLookup = new MockBlockLookup(createVerifiableBlock(8), new BlockChainScore(10), remoteChain);
		remoteBlockLookup.addBlock(createVerifiableBlock(5));

		// Act:
		final ComparisonResult result = comparer.compare(new MockBlockLookup(createVerifiableBlock(8), localChain), remoteBlockLookup);

		// Assert:
		MatcherAssert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		MatcherAssert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(4L));
		MatcherAssert.assertThat(result.areChainsConsistent(), IsEqual.equalTo(false));
	}

	@Test
	public void remoteIsNotSyncedIfSecondDifferentRemoteBlockIsNotVerifiable() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer(5);

		final byte[][] commonHashes = new byte[][]{
				Utils.generateRandomBytes(32), Utils.generateRandomBytes(32)
		};
		final HashChain localChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));
		final HashChain remoteChain = createHashChain(commonHashes, Utils.generateRandomBytes(32));

		final MockBlockLookup remoteBlockLookup = new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8),
				new BlockChainScore(10), remoteChain);
		remoteBlockLookup.addBlock(createVerifiableBlock(5));
		remoteBlockLookup.addBlock(createNonVerifiableBlock(6));

		// Act:
		final ComparisonResult result = comparer.compare(new MockBlockLookup(createVerifiableBlock(8), localChain), remoteBlockLookup);

		// Assert:
		MatcherAssert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		MatcherAssert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(4L));
		MatcherAssert.assertThat(result.areChainsConsistent(), IsEqual.equalTo(false));
	}

	// endregion

	// region utils

	private static HashChain createHashChain(final byte[]... hashes) {
		final HashChain chain = new HashChain(hashes.length);
		for (final byte[] hash : hashes) {
			chain.add(new Hash(hash));
		}

		return chain;
	}

	private static HashChain createHashChain(final byte[][] hashes, final byte[] additionalHash) {
		final HashChain chain = createHashChain(hashes);
		chain.add(new Hash(additionalHash));
		return chain;
	}

	private static Block createVerifiableBlock(final long height) {
		return createVerifiableBlock(Utils.generateRandomAccount(), height);
	}

	private static Block createVerifiableBlock(final Account account, final long height) {
		final Block block = new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
		block.sign();
		return block;
	}

	private static Block createNonVerifiableBlock(final long height) {
		return createNonVerifiableBlock(Utils.generateRandomAccount(), height);
	}

	private static Block createNonVerifiableBlock(final Account account, final long height) {
		final Block block = new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
		block.setSignature(new Signature(Utils.generateRandomBytes(64)));
		return block;
	}

	private static BlockChainComparer createBlockChainComparer(final int maxNumBlocksToRewrite) {
		final ComparisonContext context = new ComparisonContext(20, maxNumBlocksToRewrite);
		return new BlockChainComparer(context);
	}

	private static BlockChainComparer createBlockChainComparer() {
		return createBlockChainComparer(10);
	}

	// endregion
}

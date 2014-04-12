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
				new MockBlockLookup(null));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.REMOTE_HAS_NO_BLOCKS));
	}

	@Test
	public void remoteHasNonVerifiableBlocksIfRemoteLastBlockDoesNotVerify() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 7)),
				new MockBlockLookup(createNonVerifiableBlock(Utils.generateRandomAccount(), 7)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.REMOTE_HAS_NON_VERIFIABLE_BLOCK));
	}

	@Test
	public void remoteIsSyncedIfLocalAndRemoteChainsHaveSameLastBlock() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(account, 7)),
				new MockBlockLookup(createVerifiableBlock(account, 7)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.REMOTE_IS_SYNCED));
	}

	@Test
	public void remoteIsNotSyncedIfLocalAndRemoteChainsHaveDifferentLastBlock() {
		// Arrange:
		final BlockChainComparer comparer = createBlockChainComparer();

		// Act:
		int result = comparer.compare(
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8)),
				new MockBlockLookup(createVerifiableBlock(Utils.generateRandomAccount(), 8)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ComparisonResult.REMOTE_IS_BEHIND));
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

	private static BlockChainComparer createBlockChainComparer() {
		final ComparisonContext context = new ComparisonContext(10, 20, new MockBlockScorer());
		return new BlockChainComparer(context);
	}

	private static class MockBlockLookup implements BlockLookup {

		private final Block lastBlock;

		public MockBlockLookup(final Block lastBlock) {
			this.lastBlock = lastBlock;
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
			return null;
		}
	}
}
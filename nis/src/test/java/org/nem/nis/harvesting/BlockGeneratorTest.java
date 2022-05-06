package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.*;
import org.nem.nis.validators.BlockValidator;

import java.math.BigInteger;
import java.util.*;

public class BlockGeneratorTest {

	// region generated block properties

	@Test
	public void generatedBlockHasBlockHeightOneGreaterThanLastBlockHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(8)));
	}

	@Test
	public void generatedBlockHasLastBlockAsParent() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block lastBlock = NisUtils.createRandomBlockWithHeight(7);
		final Hash lastBlockHash = HashUtils.calculateHash(lastBlock);

		// Act:
		final Block block = context.generateNextBlock(lastBlock).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(lastBlockHash));
	}

	@Test
	public void generatedBlockHasCorrectTimeStamp() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context
				.generateNextBlock(NisUtils.createRandomBlockWithHeight(7), Utils.generateRandomAccount(), new TimeInstant(17)).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(17)));
	}

	@Test
	public void generatedBlockIsSigned() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void generatedBlockHasNullLessorWhenSelfSigned() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signerAccount = context.accountCache.findByAddress(Utils.generateRandomAddress());

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7), signerAccount).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getSigner(), IsEqual.equalTo(signerAccount));
		MatcherAssert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	@Test
	public void generatedBlockHasNonNullLessorWhenRemoteAccountIsSigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account remoteAccount = Utils.generateRandomAccount();
		final Account ownerAccount = Utils.generateRandomAccount();
		Mockito.when(context.accountStateCache.findForwardedStateByAddress(Mockito.any(), Mockito.any()))
				.then(o -> new AccountState(ownerAccount.getAddress()));
		Mockito.when(context.accountCache.findByAddress(Mockito.any())).thenReturn(ownerAccount);

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7), remoteAccount).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getSigner(), IsEqual.equalTo(remoteAccount));
		MatcherAssert.assertThat(block.getLessor(), IsEqual.equalTo(ownerAccount));
		Mockito.verify(context.accountStateCache, Mockito.only()).findForwardedStateByAddress(remoteAccount.getAddress(),
				new BlockHeight(8));
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(ownerAccount.getAddress());
	}

	@Test
	public void generatedBlockCanHaveNoTransactions() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));
	}

	@Test
	public void generatedBlockCanHaveTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Transaction> transactions = Arrays.asList(new MockTransaction(7, new TimeInstant(1)),
				new MockTransaction(11, new TimeInstant(2)), new MockTransaction(5, new TimeInstant(3)));
		transactions.forEach(VerifiableEntity::sign);
		context.setBlockTransactions(transactions);

		final Account remoteAccount = Utils.generateRandomAccount();
		final Account ownerAccount = Utils.generateRandomAccount();
		Mockito.when(context.accountStateCache.findForwardedStateByAddress(Mockito.any(), Mockito.any()))
				.then(o -> new AccountState(ownerAccount.getAddress()));
		Mockito.when(context.accountCache.findByAddress(Mockito.any())).thenReturn(ownerAccount);

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(123), remoteAccount, new TimeInstant(11))
				.getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getTransactions().size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(block.getTransactions(), IsEqual.equalTo(transactions));
		Mockito.verify(context.transactionsProvider, Mockito.only()).getBlockTransactions(ownerAccount.getAddress(), new TimeInstant(11),
				new BlockHeight(124));
	}

	@Test
	public void generatedBlockHasCorrectDifficultyWhenMaxSamplesAreNotAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockDifficulty difficulty = new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() * 2);
		final List<TimeInstant> timeStamps = Arrays.asList(new TimeInstant(5), new TimeInstant(9), new TimeInstant(7));
		final List<BlockDifficulty> difficulties = Collections.singletonList(BlockDifficulty.INITIAL_DIFFICULTY);
		Mockito.when(context.blockDao.getTimeStampsFrom(Mockito.any(), Mockito.anyInt())).thenReturn(timeStamps);
		Mockito.when(context.blockDao.getDifficultiesFrom(Mockito.any(), Mockito.anyInt())).thenReturn(difficulties);
		Mockito.when(context.difficultyScorer.calculateDifficulty(Mockito.any(), Mockito.any())).thenReturn(difficulty);

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(difficulty));
		Mockito.verify(context.blockDao, Mockito.times(1)).getTimeStampsFrom(new BlockHeight(1), 7);
		Mockito.verify(context.blockDao, Mockito.times(1)).getDifficultiesFrom(new BlockHeight(1), 7);
		Mockito.verify(context.difficultyScorer, Mockito.only()).calculateDifficulty(difficulties, timeStamps);
	}

	@Test
	public void generatedBlockHasCorrectDifficultyWhenMaxSamplesAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockDifficulty difficulty = new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() * 2);
		final List<TimeInstant> timeStamps = Arrays.asList(new TimeInstant(5), new TimeInstant(9), new TimeInstant(7));
		final List<BlockDifficulty> difficulties = Collections.singletonList(BlockDifficulty.INITIAL_DIFFICULTY);
		Mockito.when(context.blockDao.getTimeStampsFrom(Mockito.any(), Mockito.anyInt())).thenReturn(timeStamps);
		Mockito.when(context.blockDao.getDifficultiesFrom(Mockito.any(), Mockito.anyInt())).thenReturn(difficulties);
		Mockito.when(context.difficultyScorer.calculateDifficulty(Mockito.any(), Mockito.any())).thenReturn(difficulty);

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(100)).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(difficulty));
		Mockito.verify(context.blockDao, Mockito.times(1)).getTimeStampsFrom(new BlockHeight(41), 60);
		Mockito.verify(context.blockDao, Mockito.times(1)).getDifficultiesFrom(new BlockHeight(41), 60);
		Mockito.verify(context.difficultyScorer, Mockito.only()).calculateDifficulty(difficulties, timeStamps);
	}

	// endregion

	// region evaluation

	@Test
	public void blockIsReturnedIfHitIsLessThanTarget() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block lastBlock = NisUtils.createRandomBlockWithHeight(7);
		Mockito.when(context.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.valueOf(5));
		Mockito.when(context.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.valueOf(6));
		Mockito.when(context.scorer.calculateBlockScore(Mockito.any(), Mockito.any())).thenReturn(1245L);

		// Act:
		final GeneratedBlock generatedBlock = context.generateNextBlock(lastBlock);
		final Block block = generatedBlock.getBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsNull.notNullValue());
		MatcherAssert.assertThat(generatedBlock.getScore(), IsEqual.equalTo(1245L));
		Mockito.verify(context.scorer, Mockito.times(1)).calculateHit(block);
		Mockito.verify(context.scorer, Mockito.times(1)).calculateTarget(lastBlock, block);
		Mockito.verify(context.scorer, Mockito.times(1)).calculateBlockScore(lastBlock, block);
		Mockito.verify(context.transactionsProvider, Mockito.only()).getBlockTransactions(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void blockIsNotReturnedIfHitIsEqualToTarget() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block lastBlock = NisUtils.createRandomBlockWithHeight(7);
		Mockito.when(context.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.valueOf(5));
		Mockito.when(context.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.valueOf(5));

		// Act:
		final GeneratedBlock generatedBlock = context.generateNextBlock(lastBlock);

		// Assert:
		MatcherAssert.assertThat(generatedBlock, IsNull.nullValue());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateHit(Mockito.any());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateTarget(Mockito.eq(lastBlock), Mockito.any());
		Mockito.verify(context.scorer, Mockito.never()).calculateBlockScore(Mockito.any(), Mockito.any());
		Mockito.verify(context.transactionsProvider, Mockito.never()).getBlockTransactions(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void blockIsNotReturnedIfHitIsGreaterThanTarget() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block lastBlock = NisUtils.createRandomBlockWithHeight(7);
		Mockito.when(context.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.valueOf(6));
		Mockito.when(context.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.valueOf(5));

		// Act:
		final GeneratedBlock generatedBlock = context.generateNextBlock(lastBlock);

		// Assert:
		MatcherAssert.assertThat(generatedBlock, IsNull.nullValue());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateHit(Mockito.any());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateTarget(Mockito.eq(lastBlock), Mockito.any());
		Mockito.verify(context.scorer, Mockito.never()).calculateBlockScore(Mockito.any(), Mockito.any());
		Mockito.verify(context.transactionsProvider, Mockito.never()).getBlockTransactions(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void blockIsReturnedIfValidationSucceeds() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block lastBlock = NisUtils.createRandomBlockWithHeight(7);
		Mockito.when(context.validator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		final GeneratedBlock generatedBlock = context.generateNextBlock(lastBlock);
		final Block block = generatedBlock.getBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsNull.notNullValue());
		Mockito.verify(context.validator, Mockito.only()).validate(block);
	}

	@Test
	public void blockIsNotReturnedIfValidationFails() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block lastBlock = NisUtils.createRandomBlockWithHeight(7);
		Mockito.when(context.validator.validate(Mockito.any())).thenReturn(ValidationResult.FAILURE_INSUFFICIENT_BALANCE);

		// Act:
		final GeneratedBlock generatedBlock = context.generateNextBlock(lastBlock);

		// Assert:
		MatcherAssert.assertThat(generatedBlock, IsNull.nullValue());
		Mockito.verify(context.validator, Mockito.only()).validate(Mockito.any());
	}

	// endregion

	// region availability of public key

	@Test
	public void generateNextBlockUsesSuppliedHarvesterAccountToFilterBlockTransactionsWhenNotRemoteHarvesting() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signerAccount = Utils.generateRandomAccount();
		final Account signerAccountWithoutPublicKey = new Account(Address.fromEncoded(signerAccount.getAddress().getEncoded()));
		Mockito.when(context.accountCache.findByAddress(Mockito.eq(signerAccount.getAddress()))).thenReturn(signerAccountWithoutPublicKey);
		Mockito.when(context.accountStateCache.findForwardedStateByAddress(Mockito.eq(signerAccount.getAddress()), Mockito.any()))
				.thenReturn(new AccountState(signerAccountWithoutPublicKey.getAddress()));

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7), signerAccount).getBlock();

		// Assert:
		MatcherAssert.assertThat(block.getSigner(), IsEqual.equalTo(signerAccount));
		MatcherAssert.assertThat(block.getLessor(), IsNull.nullValue());

		final ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
		Mockito.verify(context.transactionsProvider, Mockito.only()).getBlockTransactions(addressCaptor.capture(), Mockito.any(),
				Mockito.any());
		MatcherAssert.assertThat(addressCaptor.getValue(), IsEqual.equalTo(signerAccount.getAddress()));
		MatcherAssert.assertThat(addressCaptor.getValue().getPublicKey(), IsNull.notNullValue());
	}

	@Test
	public void generateNextBlockUsesOwningAccountToFilterBlockTransactionsWhenRemoteHarvesting() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account remoteAccount = Utils.generateRandomAccount();
		final Account ownerAccount = Utils.generateRandomAccount();
		Mockito.when(context.accountStateCache.findForwardedStateByAddress(Mockito.eq(remoteAccount.getAddress()), Mockito.any()))
				.then(o -> new AccountState(ownerAccount.getAddress()));
		Mockito.when(context.accountCache.findByAddress(Mockito.eq(ownerAccount.getAddress()))).thenReturn(ownerAccount);

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7), remoteAccount).getBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsNull.notNullValue());
		MatcherAssert.assertThat(block.getSigner(), IsEqual.equalTo(remoteAccount));
		MatcherAssert.assertThat(block.getLessor(), IsEqual.equalTo(ownerAccount));

		final ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
		Mockito.verify(context.transactionsProvider, Mockito.only()).getBlockTransactions(addressCaptor.capture(), Mockito.any(),
				Mockito.any());
		MatcherAssert.assertThat(addressCaptor.getValue(), IsEqual.equalTo(ownerAccount.getAddress()));
		MatcherAssert.assertThat(addressCaptor.getValue().getPublicKey(), IsNull.notNullValue());
	}

	// endregion

	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final ReadOnlyAccountStateCache accountStateCache = Mockito.mock(ReadOnlyAccountStateCache.class);
		private final NewBlockTransactionsProvider transactionsProvider = Mockito.mock(NewBlockTransactionsProvider.class);
		private final BlockDao blockDao = Mockito.mock(BlockDao.class);
		private final BlockDifficultyScorer difficultyScorer = Mockito.mock(BlockDifficultyScorer.class);
		private final BlockScorer scorer = Mockito.mock(BlockScorer.class);
		private final BlockValidator validator = Mockito.mock(BlockValidator.class);
		private final BlockGenerator generator = new BlockGenerator(
				NisCacheFactory.createReadOnly(this.accountCache, this.accountStateCache), this.transactionsProvider, this.blockDao,
				this.scorer, this.validator);

		private TestContext() {
			final Account signer = Utils.generateRandomAccount();
			Mockito.when(this.accountStateCache.findForwardedStateByAddress(Mockito.any(), Mockito.any()))
					.thenReturn(new AccountState(signer.getAddress()));
			Mockito.when(this.accountCache.findByAddress(Mockito.any())).thenReturn(signer);

			this.setBlockTransactions(new ArrayList<>());

			Mockito.when(this.difficultyScorer.calculateDifficulty(Mockito.any(), Mockito.any())).thenReturn(new BlockDifficulty(13));
			Mockito.when(this.scorer.getDifficultyScorer()).thenReturn(this.difficultyScorer);
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.valueOf(5));
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.valueOf(6));

			Mockito.when(this.validator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		}

		private GeneratedBlock generateNextBlock(final Block lastBlock) {
			return this.generateNextBlock(lastBlock, Utils.generateRandomAccount());
		}

		private GeneratedBlock generateNextBlock(final Block lastBlock, final Account harvesterSigner) {
			return this.generateNextBlock(lastBlock, harvesterSigner, new TimeInstant(7));
		}

		private GeneratedBlock generateNextBlock(final Block lastBlock, final Account harvesterSigner, final TimeInstant timeInstant) {
			return this.generator.generateNextBlock(lastBlock, harvesterSigner, timeInstant);
		}

		private void setBlockTransactions(final List<Transaction> transactions) {
			Mockito.when(this.transactionsProvider.getBlockTransactions(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(transactions);
		}
	}
}

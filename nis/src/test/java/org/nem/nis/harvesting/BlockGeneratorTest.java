package org.nem.nis.harvesting;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.BlockValidator;

import java.math.BigInteger;
import java.util.*;

public class BlockGeneratorTest {

	//region generated block properties

	@Test
	public void generatedBlockHasBlockHeightOneGreaterThanLastBlockHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(8)));
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
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(lastBlockHash));
	}

	@Test
	public void generatedBlockHasCorrectTimeStamp() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context.generateNextBlock(
				NisUtils.createRandomBlockWithHeight(7),
				Utils.generateRandomAccount(),
				new TimeInstant(17)).getBlock();

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(17)));
	}

	@Test
	public void generatedBlockIsSigned() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		Assert.assertThat(block.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void generatedBlockHasNullLessorWhenSelfSigned() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account signerAccount = context.accountCache.findByAddress(Utils.generateRandomAddress());

		// Act:
		final Block block = context.generateNextBlock(
				NisUtils.createRandomBlockWithHeight(7),
				signerAccount).getBlock();

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signerAccount));
		Assert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	@Test
	public void generatedBlockHasNonNullLessorWhenRemoteAccountIsSigner() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account remoteAccount = Utils.generateRandomAccount();
		final Account ownerAccount = Utils.generateRandomAccount();
		Mockito.when(context.poiFacade.findForwardedStateByAddress(Mockito.any(), Mockito.any()))
				.then(o -> new AccountState(ownerAccount.getAddress()));
		Mockito.when(context.accountCache.findByAddress(Mockito.any())).thenReturn(ownerAccount);

		// Act:
		final Block block = context.generateNextBlock(
				NisUtils.createRandomBlockWithHeight(7),
				remoteAccount).getBlock();

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(remoteAccount));
		Assert.assertThat(block.getLessor(), IsEqual.equalTo(ownerAccount));
		Mockito.verify(context.poiFacade, Mockito.only()).findForwardedStateByAddress(remoteAccount.getAddress(), new BlockHeight(8));
		Mockito.verify(context.accountCache, Mockito.only()).findByAddress(ownerAccount.getAddress());
	}

	@Test
	public void generatedBlockCanHaveNoTransactions() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));
	}

	@Test
	public void generatedBlockCanHaveTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Transaction> transactions = Arrays.asList(
				new MockTransaction(7, new TimeInstant(1)),
				new MockTransaction(11, new TimeInstant(2)),
				new MockTransaction(5, new TimeInstant(3)));
		transactions.forEach(VerifiableEntity::sign);
		context.setBlockTransactions(transactions);

		final Account remoteAccount = Utils.generateRandomAccount();
		final Account ownerAccount = Utils.generateRandomAccount();
		Mockito.when(context.poiFacade.findForwardedStateByAddress(Mockito.any(), Mockito.any()))
				.then(o -> new AccountState(ownerAccount.getAddress()));
		Mockito.when(context.accountCache.findByAddress(Mockito.any())).thenReturn(ownerAccount);

		// Act:
		final Block block = context.generateNextBlock(
				NisUtils.createRandomBlockWithHeight(BlockMarkerConstants.BETA_TX_COUNT_FORK + 1),
				remoteAccount,
				new TimeInstant(11)).getBlock();

		// Assert:
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(3));
		Assert.assertThat(block.getTransactions(), IsEqual.equalTo(transactions));
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(1))
				.getTransactionsForNewBlock(ownerAccount.getAddress(), new TimeInstant(11));
	}

	@Test
	public void generatedBlockHasCorrectDifficultyWhenMaxSamplesAreNotAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockDifficulty difficulty = new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() * 2);
		final List<TimeInstant> timeStamps = Arrays.asList(new TimeInstant(5), new TimeInstant(9), new TimeInstant(7));
		final List<BlockDifficulty> difficulties = Arrays.asList(BlockDifficulty.INITIAL_DIFFICULTY);
		Mockito.when(context.blockDao.getTimeStampsFrom(Mockito.any(), Mockito.anyInt())).thenReturn(timeStamps);
		Mockito.when(context.blockDao.getDifficultiesFrom(Mockito.any(), Mockito.anyInt())).thenReturn(difficulties);
		Mockito.when(context.difficultyScorer.calculateDifficulty(Mockito.any(), Mockito.any(), Mockito.anyLong()))
				.thenReturn(difficulty);

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(7)).getBlock();

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(difficulty));
		Mockito.verify(context.blockDao, Mockito.times(1)).getTimeStampsFrom(new BlockHeight(1), 7);
		Mockito.verify(context.blockDao, Mockito.times(1)).getDifficultiesFrom(new BlockHeight(1), 7);
		Mockito.verify(context.difficultyScorer, Mockito.only()).calculateDifficulty(difficulties, timeStamps, 8);
	}

	@Test
	public void generatedBlockHasCorrectDifficultyWhenMaxSamplesAreAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockDifficulty difficulty = new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() * 2);
		final List<TimeInstant> timeStamps = Arrays.asList(new TimeInstant(5), new TimeInstant(9), new TimeInstant(7));
		final List<BlockDifficulty> difficulties = Arrays.asList(BlockDifficulty.INITIAL_DIFFICULTY);
		Mockito.when(context.blockDao.getTimeStampsFrom(Mockito.any(), Mockito.anyInt())).thenReturn(timeStamps);
		Mockito.when(context.blockDao.getDifficultiesFrom(Mockito.any(), Mockito.anyInt())).thenReturn(difficulties);
		Mockito.when(context.difficultyScorer.calculateDifficulty(Mockito.any(), Mockito.any(), Mockito.anyLong()))
				.thenReturn(difficulty);

		// Act:
		final Block block = context.generateNextBlock(NisUtils.createRandomBlockWithHeight(100)).getBlock();

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(difficulty));
		Mockito.verify(context.blockDao, Mockito.times(1)).getTimeStampsFrom(new BlockHeight(41), 60);
		Mockito.verify(context.blockDao, Mockito.times(1)).getDifficultiesFrom(new BlockHeight(41), 60);
		Mockito.verify(context.difficultyScorer, Mockito.only()).calculateDifficulty(difficulties, timeStamps, 101);
	}

	//endregion

	//region evaluation

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
		Assert.assertThat(block, IsNull.notNullValue());
		Assert.assertThat(generatedBlock.getScore(), IsEqual.equalTo(1245L));
		Mockito.verify(context.scorer, Mockito.times(1)).calculateHit(block);
		Mockito.verify(context.scorer, Mockito.times(1)).calculateTarget(lastBlock, block);
		Mockito.verify(context.scorer, Mockito.times(1)).calculateBlockScore(lastBlock, block);
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
		Assert.assertThat(generatedBlock, IsNull.nullValue());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateHit(Mockito.any());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateTarget(Mockito.eq(lastBlock), Mockito.any());
		Mockito.verify(context.scorer, Mockito.never()).calculateBlockScore(Mockito.any(), Mockito.any());
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
		Assert.assertThat(generatedBlock, IsNull.nullValue());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateHit(Mockito.any());
		Mockito.verify(context.scorer, Mockito.times(1)).calculateTarget(Mockito.eq(lastBlock), Mockito.any());
		Mockito.verify(context.scorer, Mockito.never()).calculateBlockScore(Mockito.any(), Mockito.any());
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
		Assert.assertThat(block, IsNull.notNullValue());
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
		Assert.assertThat(generatedBlock, IsNull.nullValue());
		Mockito.verify(context.validator, Mockito.only()).validate(Mockito.any());
	}

	//endregion

	//region side-effects

	@Test
	public void generateNextBlockDropsExpiredTransactions() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.generateNextBlock(
				NisUtils.createRandomBlockWithHeight(7),
				Utils.generateRandomAccount(),
				new TimeInstant(22));

		// Assert:
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(1)).dropExpiredTransactions(new TimeInstant(22));
	}

	//endregion

	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final DefaultPoiFacade poiFacade = Mockito.mock(DefaultPoiFacade.class);
		private final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
		private final BlockDao blockDao = Mockito.mock(BlockDao.class);
		private final BlockDifficultyScorer difficultyScorer = Mockito.mock(BlockDifficultyScorer.class);
		private final BlockScorer scorer = Mockito.mock(BlockScorer.class);
		private final BlockValidator validator = Mockito.mock(BlockValidator.class);
		private final BlockGenerator generator = new BlockGenerator(
				NisUtils.createNisCache(this.accountCache, this.poiFacade),
				this.unconfirmedTransactions,
				this.blockDao,
				this.scorer,
				this.validator);

		private TestContext() {
			final Account signer = Utils.generateRandomAccount();
			Mockito.when(this.poiFacade.findForwardedStateByAddress(Mockito.any(), Mockito.any()))
					.thenReturn(new AccountState(signer.getAddress()));
			Mockito.when(this.accountCache.findByAddress(Mockito.any())).thenReturn(signer);

			this.setBlockTransactions(new ArrayList<>());

			Mockito.when(this.difficultyScorer.calculateDifficulty(Mockito.any(), Mockito.any(), Mockito.anyLong())).thenReturn(new BlockDifficulty(13));
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
			final UnconfirmedTransactions filteredTransactions = Mockito.mock(UnconfirmedTransactions.class);
			Mockito.when(this.unconfirmedTransactions.getTransactionsForNewBlock(Mockito.any(), Mockito.any())).thenReturn(filteredTransactions);
			Mockito.when(filteredTransactions.getMostImportantTransactions(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK))
					.thenReturn(transactions);
		}
	}
}
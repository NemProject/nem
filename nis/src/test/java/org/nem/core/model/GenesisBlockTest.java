package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.time.TimeInstant;

public class GenesisBlockTest {

	private final GenesisBlock GENESIS_BLOCK = GenesisBlock.create();
	private final static String GENESIS_ACCOUNT = NetworkInfo.getDefault().getGenesisAccountId();

	@Test
	public void genesisBlockCanBeCreated() {
		// Act:
		final Block block = GENESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.getSigner().getAddress().getEncoded(), IsEqual.equalTo(GENESIS_ACCOUNT));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(TimeInstant.ZERO));

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(Hash.ZERO));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(9));

		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
		Assert.assertThat(block.getGenerationHash(), IsNull.notNullValue());
	}

	@Test
	public void genesisBlockIsVerifiable() {
		// Arrange:
		final Block block = GENESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void genesisTransactionsAreVerifiable() {
		// Arrange:
		final Block block = GENESIS_BLOCK;

		// Assert:
		for (final Transaction transaction : block.getTransactions())
			Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void genesisTransactionsDoNotHaveFees() {
		// Arrange:
		final Block block = GENESIS_BLOCK;

		// Assert:
		for (final Transaction transaction : block.getTransactions())
			Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.ZERO));
	}

	//region constants

	@Test
	public void amountConstantIsConsistentWithGenesisBlock() {
		// Act:
		Amount totalAmount = Amount.ZERO;
		final Block block = GENESIS_BLOCK;
		for (final Transaction transaction : block.getTransactions())
			totalAmount = totalAmount.add(((TransferTransaction)transaction).getAmount());

		// Assert:
		Assert.assertThat(totalAmount, IsEqual.equalTo(GenesisBlock.AMOUNT));
	}

	@Test
	public void accountConstantIsConsistentWithGenesisBlock() {
		// Arrange:
		final Block block = GENESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(GenesisBlock.ACCOUNT));
	}

	//endregion
}

package org.nem.core.model;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.time.TimeInstant;

import java.io.*;

public class GenesisBlockTest {

	private final GenesisBlock GENESIS_BLOCK = GenesisBlock.fromResource();
	private final static String GENESIS_ACCOUNT = NetworkInfo.getDefault().getGenesisAccountId();

	@Test
	public void genesisBlockCanBeCreated() {
		// Act:
		final Block block = GENESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.getSigner().getAddress().getEncoded(), IsEqual.equalTo(GENESIS_ACCOUNT));
		Assert.assertThat(block.getType(), IsEqual.equalTo(-1));
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
		Assert.assertThat(block.getSigner().getAddress(), IsEqual.equalTo(GenesisBlock.ADDRESS));
	}

	//endregion

	//region failed construction

	@Test(expected = IllegalArgumentException.class)
	public void genesisBlockCannotBeLoadedWithIncorrectType() {
		// Arrange:
		final JSONObject genesisBlockJson = loadGenesisBlockJsonObject();
		genesisBlockJson.put("type", 1);

		// Act:
		GenesisBlock.fromJsonObject(genesisBlockJson);
	}

	@Test(expected = IllegalArgumentException.class)
	public void genesisBlockCannotBeLoadedFromInvalidJson() throws IOException {
		// Arrange:
		final JSONObject genesisBlockJson = loadGenesisBlockJsonObject();
		final String badJson = "<bad>" + genesisBlockJson.toJSONString();
		try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(badJson.getBytes())) {
			// Act:
			GenesisBlock.fromStream(inputStream);
		}
	}

	private static JSONObject loadGenesisBlockJsonObject() {
		try (final InputStream fin = GenesisBlock.class.getClassLoader().getResourceAsStream("genesis-block.json")) {
			return (JSONObject)JSONValue.parseStrict(fin);
		}
		catch (IOException|net.minidev.json.parser.ParseException e) {
			Assert.fail("unexpected exception was thrown when parsing genesis block resource");
			return null;
		}
	}

	//endregion
}

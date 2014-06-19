package org.nem.nis;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.AccountAnalyzer;

import java.io.*;

// Test temporarily moved from org.nem.core to org.nem.nis, due to usagen of AA

public class NemesisBlockTest {

	private final NemesisBlock NEMESIS_BLOCK = NemesisBlock.fromResource((new AccountAnalyzer(null)).asAutoCache());
	private final static String NEMESIS_ACCOUNT = NetworkInfo.getDefault().getNemesisAccountId();

	@Test
	public void nemesisBlockCanBeCreated() {
		// Act:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.getSigner().getAddress().getEncoded(), IsEqual.equalTo(NEMESIS_ACCOUNT));
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
	public void nemesisBlockIsVerifiable() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void nemesisTransactionsAreVerifiable() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		for (final Transaction transaction : block.getTransactions())
			Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void nemesisTransactionsDoNotHaveFees() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		for (final Transaction transaction : block.getTransactions())
			Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.ZERO));
	}

	//region constants

	@Test
	public void amountConstantIsConsistentWithNemesisBlock() {
		// Act:
		Amount totalAmount = Amount.ZERO;
		final Block block = NEMESIS_BLOCK;
		for (final Transaction transaction : block.getTransactions())
			totalAmount = totalAmount.add(((TransferTransaction)transaction).getAmount());

		// Assert:
		Assert.assertThat(totalAmount, IsEqual.equalTo(NemesisBlock.AMOUNT));
	}

	@Test
	public void accountConstantIsConsistentWithNemesisBlock() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.getSigner().getAddress(), IsEqual.equalTo(NemesisBlock.ADDRESS));
	}

	//endregion

	//region failed construction

	@Test(expected = IllegalArgumentException.class)
	public void nemesisBlockCannotBeLoadedWithIncorrectType() {
		// Arrange:
		final JSONObject nemesisBlockJson = loadNemesisBlockJsonObject();
		nemesisBlockJson.put("type", 1);

		// Act:
		NemesisBlock.fromJsonObject(nemesisBlockJson, (new AccountAnalyzer(null)).asAutoCache());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nemesisBlockCannotBeLoadedFromInvalidJson() throws IOException {
		// Arrange:
		final JSONObject nemesisBlockJson = loadNemesisBlockJsonObject();
		final String badJson = "<bad>" + nemesisBlockJson.toJSONString();
		try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(badJson.getBytes())) {
			// Act:
			NemesisBlock.fromStream(inputStream, (new AccountAnalyzer(null)).asAutoCache());
		}
	}

	private static JSONObject loadNemesisBlockJsonObject() {
		try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream("nemesis-block.json")) {
			return (JSONObject)JSONValue.parseStrict(fin);
		}
		catch (IOException|net.minidev.json.parser.ParseException e) {
			Assert.fail("unexpected exception was thrown when parsing nemesis block resource");
			return null;
		}
	}

	//endregion
}

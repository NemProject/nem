package org.nem.core.model;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.time.TimeInstant;

import java.io.*;

// TODO 20150108 J-G: it looks like the nemesis block isn't verifiable :/
// > there is also something wrong in that the nemesis transactions shouldn't have fees

public class NemesisBlockTest {

	private final static MockAccountLookup MOCK_ACCOUNT_LOOKUP = new MockAccountLookup();
	private final static NemesisBlock NEMESIS_BLOCK = NemesisBlock.fromResource(new DeserializationContext(MOCK_ACCOUNT_LOOKUP));
	private final static String NEMESIS_ACCOUNT = NetworkInfo.getDefault().getNemesisAccountId();
	private final static int NUM_NEMESIS_TRANSACTIONS = 162;

	//region basic

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
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(NUM_NEMESIS_TRANSACTIONS));

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
		for (final Transaction transaction : block.getTransactions()) {
			Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
		}
	}

	@Test
	public void nemesisTransactionsDoNotHaveFees() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		for (final Transaction transaction : block.getTransactions()) {
			if (transaction instanceof TransferTransaction) {
				Assert.assertThat(transaction.getFee(), IsEqual.equalTo(Amount.ZERO));

			} else {
				Assert.assertThat(transaction.getFee(), IsEqual.equalTo(transaction.getMinimumFee()));
			}
		}
	}

	@Test
	public void nemesisDeserializationUsesAccountLookupParameter() {
		// Assert: (1 signer, 2 multisig senders, NUM_NEMESIS_TRANSACTIONS senders, NUM_NEMESIS_TRANSACTIONS recipients)
		Assert.assertThat(MOCK_ACCOUNT_LOOKUP.getNumFindByIdCalls(), IsEqual.equalTo(1 + 2 + 2 * NUM_NEMESIS_TRANSACTIONS));
	}

	@Test
	public void nemesisAddressesAreValid() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		for (final Transaction otx : block.getTransactions()) {
			if (otx.getType() == TransactionTypes.TRANSFER) {
				final TransferTransaction tx = (TransferTransaction)otx;
				Assert.assertTrue(tx.getRecipient().getAddress().isValid());
			}
		}
	}
	//endregion

	//region constants

	@Test
	public void amountConstantIsConsistentWithNemesisBlock() {
		// Act:
		Amount totalAmount = Amount.ZERO;
		final Block block = NEMESIS_BLOCK;
		for (final Transaction transaction : block.getTransactions()) {
			if (transaction instanceof TransferTransaction) {
				totalAmount = totalAmount.add(((TransferTransaction)transaction).getAmount());
			}
		}

		// Assert:
		Assert.assertThat(totalAmount, IsEqual.equalTo(NemesisBlock.AMOUNT));
	}

	@Test
	public void addressConstantIsConsistentWithNemesisBlock() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.getSigner().getAddress(), IsEqual.equalTo(NemesisBlock.ADDRESS));
	}

	@Test
	public void generationHashConstantIsConsistentWithNemesisBlock() {
		// Arrange:
		final Block block = NEMESIS_BLOCK;

		// Assert:
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(NemesisBlock.GENERATION_HASH));
	}

	//endregion

	//region failed construction

	@Test(expected = IllegalArgumentException.class)
	public void nemesisBlockCannotBeLoadedWithIncorrectType() {
		// Arrange:
		final JSONObject nemesisBlockJson = loadNemesisBlockJsonObject();
		nemesisBlockJson.put("type", 1);

		// Act:
		NemesisBlock.fromJsonObject(nemesisBlockJson, new DeserializationContext(new MockAccountLookup()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nemesisBlockCannotBeLoadedFromInvalidJson() throws IOException {
		// Arrange:
		final JSONObject nemesisBlockJson = loadNemesisBlockJsonObject();
		final String badJson = "<bad>" + nemesisBlockJson.toJSONString();
		try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(badJson.getBytes())) {
			// Act:
			NemesisBlock.fromStream(inputStream, new DeserializationContext(new MockAccountLookup()));
		}
	}

	private static JSONObject loadNemesisBlockJsonObject() {
		try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream("nemesis-block.json")) {
			return (JSONObject)JSONValue.parseStrict(fin);
		} catch (IOException | net.minidev.json.parser.ParseException e) {
			Assert.fail("unexpected exception was thrown when parsing nemesis block resource");
			return null;
		}
	}

	//endregion
}

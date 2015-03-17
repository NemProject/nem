package org.nem.core.model;

import net.minidev.json.*;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class NemesisBlockTest {
	private final static String NEMESIS_ACCOUNT = NetworkInfos.getDefault().getNemesisBlockInfo().getAddress().getEncoded();
	private final static int NUM_NEMESIS_TRANSACTIONS = 162;
	private final static Amount EXPECTED_MULTISIG_AGGREGATE_FEE = Amount.fromNem(2 * (5 + 3 * 2)); // each with two cosignatories

	private static abstract class AbstractNemesisBlockTest {

		// basic

		@Test
		public void nemesisBlockCanBeCreated() {
			// Act:
			final Block block = this.loadNemesisBlock();

			// Assert:
			Assert.assertThat(block.getSigner().getAddress().getEncoded(), IsEqual.equalTo(NEMESIS_ACCOUNT));
			Assert.assertThat(block.getType(), IsEqual.equalTo(-1));
			Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
			Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(TimeInstant.ZERO));

			// 2 multisig aggregate transactions
			Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(EXPECTED_MULTISIG_AGGREGATE_FEE.multiply(2)));
			Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(Hash.ZERO));
			Assert.assertThat(block.getHeight(), IsEqual.equalTo(BlockHeight.ONE));
			Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(NUM_NEMESIS_TRANSACTIONS));

			Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
			Assert.assertThat(block.getGenerationHash(), IsNull.notNullValue());
		}

		@Test
		public void nemesisBlockIsVerifiable() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Assert:
			Assert.assertThat(block.verify(), IsEqual.equalTo(true));
		}

		@Test
		public void nemesisTransactionsAreVerifiable() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Assert:
			for (final Transaction transaction : block.getTransactions()) {
				Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
			}
		}

		@Test
		public void nemesisTransactionsHaveCorrectFees() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Assert:
			for (final Transaction transaction : block.getTransactions()) {
				final Amount expectedFee = TransactionTypes.TRANSFER == transaction.getType()
						? Amount.ZERO
						: EXPECTED_MULTISIG_AGGREGATE_FEE;
				Assert.assertThat(transaction.getFee(), IsEqual.equalTo(expectedFee));
			}
		}

		@Test
		public void nemesisDeserializationUsesAccountLookupParameter() {
			// Arrange:
			final MockAccountLookup accountLookup = new MockAccountLookup();
			final Block block = this.loadNemesisBlock(accountLookup);

			// Assert: (1 signer, 2 multisig senders, NUM_NEMESIS_TRANSACTIONS senders, NUM_NEMESIS_TRANSACTIONS recipients)
			Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1 + 2 + 2 * NUM_NEMESIS_TRANSACTIONS));
		}

		@Test
		public void nemesisAddressesAreValid() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Act:
			final Set<Address> allAddresses = block.getTransactions().stream()
					.flatMap(t -> t.getAccounts().stream().map(Account::getAddress))
					.collect(Collectors.toSet());

			// Assert:
			for (final Address address : allAddresses) {
				Assert.assertThat(address.toString(), address.isValid(), IsEqual.equalTo(true));
			}
		}

		//endregion

		//region constants

		// TODO 20150316 J-J: need to fix these tests

//		@Test
//		public void amountConstantIsConsistentWithNemesisBlock() {
//			// Act:
//			Amount totalAmount = Amount.ZERO;
//			final Block block = this.loadNemesisBlock();
//			for (final Transaction transaction : block.getTransactions()) {
//				if (transaction instanceof TransferTransaction) {
//					totalAmount = totalAmount.add(((TransferTransaction)transaction).getAmount());
//				}
//			}
//
//			// Assert:
//			Assert.assertThat(totalAmount, IsEqual.equalTo(NemesisBlock.AMOUNT));
//		}
//
//		@Test
//		public void addressConstantIsConsistentWithNemesisBlock() {
//			// Arrange:
//			final Block block = this.loadNemesisBlock();
//			final Address blockAddress = block.getSigner().getAddress();
//
//			// Assert:
//			Assert.assertThat(blockAddress, IsEqual.equalTo(NemesisBlock.ADDRESS));
//			Assert.assertThat(blockAddress.getPublicKey(), IsEqual.equalTo(NemesisBlock.ADDRESS.getPublicKey()));
//			Assert.assertThat(blockAddress.getPublicKey(), IsNull.notNullValue());
//		}
//
//		@Test
//		public void generationHashConstantIsConsistentWithNemesisBlock() {
//			// Arrange:
//			final Block block = this.loadNemesisBlock();
//
//			// Assert:
//			Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(NemesisBlock.GENERATION_HASH));
//		}

		//endregion

		protected abstract NemesisBlock loadNemesisBlock(final MockAccountLookup accountLookup);

		protected NemesisBlock loadNemesisBlock() {
			return this.loadNemesisBlock(new MockAccountLookup());
		}
	}

	//region basic

	public static class ResourceNemesisBlockTest extends AbstractNemesisBlockTest {

		@Override
		protected NemesisBlock loadNemesisBlock(final MockAccountLookup accountLookup) {
			return NemesisBlock.fromResource(new DeserializationContext(accountLookup));
		}
	}

	public static class JsonNemesisBlockTest extends AbstractNemesisBlockTest {

		@Test(expected = IllegalArgumentException.class)
		public void nemesisBlockCannotBeLoadedWithIncorrectType() {
			// Arrange:
			final JSONObject nemesisBlockJson = loadNemesisBlockJsonObject();
			nemesisBlockJson.put("type", 1);

			// Act:
			NemesisBlock.fromJsonObject(nemesisBlockJson, new DeserializationContext(new MockAccountLookup()));
		}

		@Override
		protected NemesisBlock loadNemesisBlock(final MockAccountLookup accountLookup) {
			final JSONObject jsonObject = loadNemesisBlockJsonObject();
			return NemesisBlock.fromJsonObject(jsonObject, new DeserializationContext(accountLookup));
		}

		private static JSONObject loadNemesisBlockJsonObject() {
			try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream("nemesis-block.json")) {
				return (JSONObject)JSONValue.parseStrict(fin);
			} catch (IOException | net.minidev.json.parser.ParseException e) {
				Assert.fail("unexpected exception was thrown when parsing nemesis block resource");
				return null;
			}
		}
	}

	public static class BinaryNemesisBlockTest extends AbstractNemesisBlockTest {

		@Test
		public void nemesisBlockCannotBeLoadedFromBlobWithIncorrectType() {
			// Arrange (set type to 1):
			final byte[] buffer = loadNemesisBlockBlobObject();
			buffer[0] = 1;
			buffer[1] = 0;
			buffer[2] = 0;
			buffer[3] = 0;

			// Act:
			ExceptionAssert.assertThrows(
					v -> NemesisBlock.fromBlobObject(buffer, new DeserializationContext(new MockAccountLookup())),
					IllegalArgumentException.class);
		}

		@Test
		public void nemesisBlockCannotBeLoadedFromInvalidBlob() {
			// Arrange:
			final byte[] buffer = loadNemesisBlockBlobObject();
			final byte[] badBuffer1 = ByteBuffer.allocate(3 + buffer.length)
					.put("bad".getBytes())
					.put(buffer)
					.array();
			final byte[] badBuffer2 = ByteBuffer.allocate(3 + buffer.length)
					.put(Arrays.copyOfRange(buffer, 0, 100))
					.put("bad".getBytes())
					.put(Arrays.copyOfRange(buffer, 100, buffer.length))
					.array();

			// Act:
			ExceptionAssert.assertThrows(
					v -> NemesisBlock.fromBlobObject(badBuffer1, new DeserializationContext(new MockAccountLookup())),
					IllegalArgumentException.class);
			ExceptionAssert.assertThrows(
					v -> NemesisBlock.fromBlobObject(badBuffer2, new DeserializationContext(new MockAccountLookup())),
					SerializationException.class);
		}

		@Override
		protected NemesisBlock loadNemesisBlock(final MockAccountLookup accountLookup) {
			final byte[] blob = loadNemesisBlockBlobObject();
			return NemesisBlock.fromBlobObject(blob, new DeserializationContext(accountLookup));
		}

		private static byte[] loadNemesisBlockBlobObject() {
			try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream("nemesis-block.bin")) {
				return IOUtils.toByteArray(fin);
			} catch (final IOException e) {
				Assert.fail("unexpected exception was thrown when parsing nemesis block resource");
				return null;
			}
		}
	}
}

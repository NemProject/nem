package org.nem.core.model;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import net.minidev.json.*;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

@RunWith(Enclosed.class)
public class NemesisBlockTest {
	private static final NemesisBlockInfo NEMESIS_BLOCK_INFO = NetworkInfos.getDefault().getNemesisBlockInfo();
	private static final int NUM_NEMESIS_TRANSACTIONS = 20;
	private static final Amount EXPECTED_MULTISIG_AGGREGATE_FEE = Amount.ZERO; // no cosigners in the new testnet
	private static final int EXPECTED_VERSION = 0x98000001;

	@BeforeClass
	public static void initNetwork() {
		NetworkInfos.setDefault(NetworkInfos.getTestNetworkInfo());
	}

	@AfterClass
	public static void resetNetwork() {
		NetworkInfos.setDefault(null);
	}

	private static abstract class AbstractNemesisBlockTest {

		// basic

		@Test
		public void nemesisBlockCanBeCreated() {
			// Act:
			final Block block = this.loadNemesisBlock();

			// Assert:
			MatcherAssert.assertThat(block.getSigner().getAddress(), IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAddress()));
			MatcherAssert.assertThat(block.getType(), IsEqual.equalTo(-1));
			MatcherAssert.assertThat(block.getVersion(), IsEqual.equalTo(EXPECTED_VERSION));
			MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(TimeInstant.ZERO));

			// no multisig aggregate transactions
			MatcherAssert.assertThat(block.getTotalFee(), IsEqual.equalTo(EXPECTED_MULTISIG_AGGREGATE_FEE));
			MatcherAssert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(Hash.ZERO));
			MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(BlockHeight.ONE));
			MatcherAssert.assertThat(block.getTransactions().size(), IsEqual.equalTo(NUM_NEMESIS_TRANSACTIONS));

			MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
			MatcherAssert.assertThat(block.getGenerationHash(), IsNull.notNullValue());
		}

		@Test
		public void nemesisBlockIsVerifiable() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Assert:
			MatcherAssert.assertThat(block.verify(), IsEqual.equalTo(true));
		}

		@Test
		public void nemesisTransactionsAreVerifiable() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Assert:
			for (final Transaction transaction : block.getTransactions()) {
				MatcherAssert.assertThat(transaction.verify(), IsEqual.equalTo(true));
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
				MatcherAssert.assertThat(transaction.getFee(), IsEqual.equalTo(expectedFee));
			}
		}

		@Test
		public void nemesisAddressesAreValid() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Act:
			final Set<Address> allAddresses = block.getTransactions().stream()
					.flatMap(t -> t.getAccounts().stream().map(Account::getAddress)).collect(Collectors.toSet());

			// Assert:
			for (final Address address : allAddresses) {
				MatcherAssert.assertThat(address.toString(), address.isValid(), IsEqual.equalTo(true));
			}
		}

		@Test
		public void nemesisTransactionSignersHavePublicKeys() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Act:
			final Set<Address> signerAddresses = block.getTransactions().stream().map(t -> t.getSigner().getAddress())
					.collect(Collectors.toSet());

			// Assert:
			for (final Address address : signerAddresses) {
				MatcherAssert.assertThat(address.getPublicKey(), IsNull.notNullValue());
			}
		}

		// endregion

		// region constants

		@Test
		public void amountConstantIsConsistentWithNemesisBlock() {
			// Act:
			Amount totalAmount = Amount.ZERO;
			final Block block = this.loadNemesisBlock();
			for (final Transaction transaction : block.getTransactions()) {
				if (transaction instanceof TransferTransaction) {
					totalAmount = totalAmount.add(((TransferTransaction) transaction).getAmount());
				}
			}

			// Assert:
			MatcherAssert.assertThat(totalAmount, IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAmount()));
		}

		@Test
		public void addressConstantIsConsistentWithNemesisBlock() {
			// Arrange:
			final Block block = this.loadNemesisBlock();
			final Address blockAddress = block.getSigner().getAddress();

			// Assert:
			MatcherAssert.assertThat(blockAddress, IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAddress()));
			MatcherAssert.assertThat(blockAddress.getPublicKey(), IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAddress().getPublicKey()));
			MatcherAssert.assertThat(blockAddress.getPublicKey(), IsNull.notNullValue());
		}

		@Test
		public void generationHashConstantIsConsistentWithNemesisBlock() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Assert:
			MatcherAssert.assertThat(block.getGenerationHash(), IsEqual.equalTo(NEMESIS_BLOCK_INFO.getGenerationHash()));
		}

		// endregion

		protected abstract Block loadNemesisBlock(final MockAccountLookup accountLookup);

		protected Block loadNemesisBlock() {
			return this.loadNemesisBlock(new MockAccountLookup());
		}
	}

	// region basic

	public static class ResourceNemesisBlockTest extends AbstractNemesisBlockTest {

		@Override
		protected Block loadNemesisBlock(final MockAccountLookup accountLookup) {
			return NemesisBlock.fromResource(NEMESIS_BLOCK_INFO, new DeserializationContext(accountLookup));
		}
	}

	public static class JsonNemesisBlockTest extends AbstractNemesisBlockTest {

		@Test(expected = IllegalArgumentException.class)
		public void nemesisBlockCannotBeLoadedWithIncorrectType() {
			// Arrange:
			final JSONObject nemesisBlockJson = loadNemesisBlockJsonObject();
			nemesisBlockJson.put("type", 1);

			// Act:
			NemesisBlock.fromJsonObject(NEMESIS_BLOCK_INFO, nemesisBlockJson, new DeserializationContext(new MockAccountLookup()));
		}

		@Override
		protected Block loadNemesisBlock(final MockAccountLookup accountLookup) {
			final JSONObject jsonObject = loadNemesisBlockJsonObject();
			return NemesisBlock.fromJsonObject(NEMESIS_BLOCK_INFO, jsonObject, new DeserializationContext(accountLookup));
		}

		private static JSONObject loadNemesisBlockJsonObject() {
			try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream("nemesis-testnet.json")) {
				return (JSONObject) JSONValue.parseStrict(new InputStreamReader(fin));
			} catch (IOException | net.minidev.json.parser.ParseException e) {
				Assert.fail("unexpected exception was thrown when parsing nemesis block resource");
				throw new RuntimeException(e);
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
					v -> NemesisBlock.fromBlobObject(NEMESIS_BLOCK_INFO, buffer, new DeserializationContext(new MockAccountLookup())),
					IllegalArgumentException.class);
		}

		@Test
		public void nemesisBlockCannotBeLoadedFromInvalidBlob() {
			// Arrange:
			final byte[] buffer = loadNemesisBlockBlobObject();
			final byte[] badBuffer1 = ByteBuffer.allocate(3 + buffer.length).put("bad".getBytes()).put(buffer).array();
			final byte[] badBuffer2 = ByteBuffer.allocate(3 + buffer.length).put(Arrays.copyOfRange(buffer, 0, 100)).put("bad".getBytes())
					.put(Arrays.copyOfRange(buffer, 100, buffer.length)).array();

			// Act:
			ExceptionAssert.assertThrows(
					v -> NemesisBlock.fromBlobObject(NEMESIS_BLOCK_INFO, badBuffer1, new DeserializationContext(new MockAccountLookup())),
					IllegalArgumentException.class);
			ExceptionAssert.assertThrows(
					v -> NemesisBlock.fromBlobObject(NEMESIS_BLOCK_INFO, badBuffer2, new DeserializationContext(new MockAccountLookup())),
					SerializationException.class);
		}

		@Override
		protected Block loadNemesisBlock(final MockAccountLookup accountLookup) {
			final byte[] blob = loadNemesisBlockBlobObject();
			return NemesisBlock.fromBlobObject(NEMESIS_BLOCK_INFO, blob, new DeserializationContext(accountLookup));
		}

		private static byte[] loadNemesisBlockBlobObject() {
			try (final InputStream fin = NemesisBlock.class.getClassLoader().getResourceAsStream(NEMESIS_BLOCK_INFO.getDataFileName())) {
				return IOUtils.toByteArray(fin);
			} catch (final IOException e) {
				throw new IllegalStateException("unexpected exception was thrown when parsing nemesis block resource");
			}
		}
	}
}

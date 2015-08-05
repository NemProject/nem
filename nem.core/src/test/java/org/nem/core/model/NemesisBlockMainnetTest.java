package org.nem.core.model;

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
public class NemesisBlockMainnetTest {
	private final static NetworkInfo NETWORK_INFO = NetworkInfos.getMainNetworkInfo();
	private final static NemesisBlockInfo NEMESIS_BLOCK_INFO = NETWORK_INFO.getNemesisBlockInfo();
	// users, devs, marketing, contributors + funds (transfer + multisig)
	private final static int NUM_NEMESIS_TRANSFER_TRANSACTIONS = 1307 + 21 + 5 + 8 + 6;
	private final static int NUM_NEMESIS_TRANSACTIONS = NUM_NEMESIS_TRANSFER_TRANSACTIONS + 6;
	private final static Amount EXPECTED_MULTISIG_AGGREGATE_FEE =
			Amount.fromNem(2 * (5 + 3 * 4) + 2 * (5 + 3 * 5) + 2 * (5 + 3 * 6));
	private final static int EXPECTED_VERSION = 0x68000001;

	@BeforeClass
	public static void initNetwork() {
		NetworkInfos.setDefault(NETWORK_INFO);
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
			Assert.assertThat(block.getSigner().getAddress(), IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAddress()));
			Assert.assertThat(block.getType(), IsEqual.equalTo(-1));
			Assert.assertThat(block.getVersion(), IsEqual.equalTo(EXPECTED_VERSION));
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
						: NemGlobals.getTransactionFeeCalculator().calculateMinimumFee(transaction);
				Assert.assertThat(transaction.getFee(), IsEqual.equalTo(expectedFee));
			}
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

		@Test
		public void nemesisTransactionSignersHavePublicKeys() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Act:
			final Set<Address> signerAddresses = block.getTransactions().stream()
					.map(t -> t.getSigner().getAddress())
					.collect(Collectors.toSet());

			// Assert:
			for (final Address address : signerAddresses) {
				Assert.assertThat(address.getPublicKey(), IsNull.notNullValue());
			}
		}

		//endregion

		//region constants

		@Test
		public void amountConstantIsConsistentWithNemesisBlock() {
			// Act:
			Amount totalAmount = Amount.ZERO;
			final Block block = this.loadNemesisBlock();
			for (final Transaction transaction : block.getTransactions()) {
				if (transaction instanceof TransferTransaction) {
					totalAmount = totalAmount.add(((TransferTransaction)transaction).getAmount());
				}
			}

			// Assert:
			Assert.assertThat(totalAmount, IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAmount()));
		}

		@Test
		public void addressConstantIsConsistentWithNemesisBlock() {
			// Arrange:
			final Block block = this.loadNemesisBlock();
			final Address blockAddress = block.getSigner().getAddress();

			// Assert:
			Assert.assertThat(blockAddress, IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAddress()));
			Assert.assertThat(blockAddress.getPublicKey(), IsEqual.equalTo(NEMESIS_BLOCK_INFO.getAddress().getPublicKey()));
			Assert.assertThat(blockAddress.getPublicKey(), IsNull.notNullValue());
		}

		@Test
		public void generationHashConstantIsConsistentWithNemesisBlock() {
			// Arrange:
			final Block block = this.loadNemesisBlock();

			// Assert:
			Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(NEMESIS_BLOCK_INFO.getGenerationHash()));
		}

		//endregion

		protected abstract Block loadNemesisBlock(final MockAccountLookup accountLookup);

		protected Block loadNemesisBlock() {
			return this.loadNemesisBlock(new MockAccountLookup());
		}
	}

	//region basic

	public static class ResourceNemesisBlockTest extends AbstractNemesisBlockTest {

		@Override
		protected Block loadNemesisBlock(final MockAccountLookup accountLookup) {
			return NemesisBlock.fromResource(NEMESIS_BLOCK_INFO, new DeserializationContext(accountLookup));
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

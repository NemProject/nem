package org.nem.nis.mappers;

import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.*;
import org.nem.nis.controller.viewmodels.ExplorerBlockViewModel;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class BlockDbModelToExplorerViewModelMappingTest {

	@Test
	public void canMapBlockToExplorerBlockViewModelWithoutTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Hash blockHash = HashUtils.calculateHash(context.block);

		// Act:
		final ExplorerBlockViewModel viewModel = context.mapping.map(context.dbBlock);

		// Assert:
		context.assertViewModel(viewModel, blockHash, 0);
	}

	@Test
	public void canMapBlockToExplorerBlockViewModelWithTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Hash> transactionHashes = context.addTransactions(3);
		final Hash blockHash = HashUtils.calculateHash(context.block);

		// Act:
		final ExplorerBlockViewModel viewModel = context.mapping.map(context.dbBlock);

		// Assert:
		context.assertViewModel(viewModel, blockHash, 3);
		MatcherAssert.assertThat(getTransactionHashes(viewModel), IsEqual.equalTo(transactionHashes));
	}

	private static Hash getDeserializedBlockHash(final JSONObject jsonObject) {
		final Block deserializedBlock = new Block(BlockTypes.REGULAR, VerifiableEntity.DeserializationOptions.VERIFIABLE,
				Utils.createDeserializer(jsonObject));
		return HashUtils.calculateHash(deserializedBlock);
	}

	private static List<Hash> getTransactionHashes(final ExplorerBlockViewModel viewModel) {
		final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);
		final JSONArray jsonTransactions = (JSONArray) jsonObject.get("txes");
		return jsonTransactions.stream().map(o -> Hash.fromHexString((String) ((JSONObject) o).get("hash"))).collect(Collectors.toList());
	}

	private static class TestContext {
		private final Hash dbBlockHash = Utils.generateRandomHash();
		private final BlockDifficulty blockDifficulty = new BlockDifficulty(BlockDifficulty.INITIAL_DIFFICULTY.getRaw() + 12345);
		private final DbBlock dbBlock = new DbBlock();
		private final Block block = NisUtils.createRandomBlockWithHeight(101);

		private final BlockDbModelToExplorerViewModelMapping mapping;

		public TestContext() {
			this.dbBlock.setBlockHash(this.dbBlockHash);
			this.block.setDifficulty(this.blockDifficulty);
			this.block.sign();

			final IMapper mapper = Mockito.mock(IMapper.class);
			Mockito.when(mapper.map(this.dbBlock, Block.class)).thenReturn(this.block);

			this.mapping = new BlockDbModelToExplorerViewModelMapping(mapper);
		}

		public List<Hash> addTransactions(final int numTransactions) {
			final List<Hash> hashes = new ArrayList<>();
			for (int i = 0; i < numTransactions; ++i) {
				final Transaction transaction = RandomTransactionFactory.createTransfer();
				transaction.sign();
				this.block.addTransaction(transaction);
				hashes.add(HashUtils.calculateHash(transaction));
			}

			return hashes;
		}

		public void assertViewModel(final ExplorerBlockViewModel viewModel, final Hash expectedBlockHash,
				final int expectedNumTransactions) {
			// Act:
			final JSONObject jsonObject = JsonSerializer.serializeToJson(viewModel);

			// Assert:
			MatcherAssert.assertThat(jsonObject.size(), IsEqual.equalTo(4));
			MatcherAssert.assertThat(getDeserializedBlockHash((JSONObject) jsonObject.get("block")), IsEqual.equalTo(expectedBlockHash));
			MatcherAssert.assertThat(jsonObject.get("hash"), IsEqual.equalTo(this.dbBlockHash.toString()));
			MatcherAssert.assertThat(jsonObject.get("difficulty"), IsEqual.equalTo(this.blockDifficulty.getRaw()));
			MatcherAssert.assertThat(((JSONArray) jsonObject.get("txes")).size(), IsEqual.equalTo(expectedNumTransactions));
		}
	}
}

package org.nem.nis.validators.integration;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.poi.GroupedHeight;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.BlockChainServices;
import org.nem.nis.test.*;

import java.util.List;

public class BlockChainServicesTransactionValidationTest extends AbstractTransactionValidationTest {

	@Override
	protected void assertTransactions(
			final ReadOnlyNisCache nisCache,
			final List<Transaction> all,
			final List<Transaction> expectedFiltered,
			final ValidationResult expectedResult) {
		// Arrange:
		final BlockChainServices blockChainServices = new BlockChainServices(
				Mockito.mock(BlockDao.class),
				new BlockTransactionObserverFactory(),
				NisUtils.createBlockValidatorFactory(),
				NisUtils.createTransactionValidatorFactory(),
				MapperUtils.createNisMapperFactory());

		final NisCache copyCache = nisCache.copy();
		final BlockHeight blockHeight = new BlockHeight(1234567);
		final Account blockSigner = createBlockSigner(copyCache, blockHeight);

		// create three blocks but put all transactions in second block
		final Block parentBlock = NisUtils.createParentBlock(blockSigner, blockHeight.getRaw());
		final List<Block> blocks = NisUtils.createBlockList(blockSigner, parentBlock, 3, parentBlock.getTimeStamp());
		blocks.get(1).addTransactions(all);
		NisUtils.signAllBlocks(blocks);

		// Act:
		final boolean result = blockChainServices.isPeerChainValid(copyCache, parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult.isSuccess()));
	}

	private static Account createBlockSigner(final NisCache nisCache, final BlockHeight blockHeight) {
		final Amount amount = Amount.fromNem(1_000_000);
		final Account blockSigner = Utils.generateRandomAccount();
		final AccountState blockSignerState = nisCache.getAccountStateCache().findStateByAddress(blockSigner.getAddress());
		blockSignerState.getImportanceInfo().setImportance(GroupedHeight.fromHeight(blockHeight), 0.1);
		blockSignerState.getAccountInfo().incrementBalance(amount);
		blockSignerState.setHeight(BlockHeight.ONE);
		blockSignerState.getWeightedBalances().addFullyVested(BlockHeight.ONE, amount);
		return blockSigner;
	}
}

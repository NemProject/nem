package org.nem.nis.test.validation;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.nis.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.secret.BlockTransactionObserverFactory;
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

		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK);
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
		blocks.get(0).addTransactions(all);
		NisUtils.resignBlocks(blocks);

		// Act:
		final boolean result = blockChainServices.isPeerChainValid(nisCache.copy(), parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult.isSuccess()));
	}
}

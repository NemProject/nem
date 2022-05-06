package org.nem.nis.service;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.ncc.TransactionMetaDataPair;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.*;

import java.util.MissingResourceException;

public class DbTransferIoAdapterTest {
	private static final long VALID_BLOCK_HEIGHT = 5;

	@Test
	public void getBlockAtDelegatesToBlockDao() {
		// Arrange:
		final TestContext context = new TestContext();
		final Hash hash = context.pair.getTransfer().getTransferHash();
		final BlockHeight height = new BlockHeight(VALID_BLOCK_HEIGHT);
		Mockito.when(context.transferDao.getTransactionUsingHash(hash, height)).thenReturn(context.pair);

		// Act:
		final TransactionMetaDataPair pair = context.transactionIo.getTransactionUsingHash(hash, height);

		// Assert:
		MatcherAssert.assertThat(pair.getEntity().getTimeStamp(), IsEqual.equalTo(new TimeInstant(123)));
		MatcherAssert.assertThat(pair.getMetaData().getHeight().getRaw(), IsEqual.equalTo(VALID_BLOCK_HEIGHT));
		MatcherAssert.assertThat(pair.getMetaData().getHash(), IsEqual.equalTo(hash));
		Mockito.verify(context.transferDao, Mockito.only()).getTransactionUsingHash(hash, height);
	}

	@Test
	public void getTransactionUsingHashThrowsExceptionIfTransactionCannotBeFound() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		ExceptionAssert.assertThrows(v -> context.transactionIo.getTransactionUsingHash(Utils.generateRandomHash(), new BlockHeight(8)),
				MissingResourceException.class);
	}

	private static class TestContext {
		private final TransferBlockPair pair = createTransferBlockPair();
		private final ReadOnlyTransferDao transferDao = Mockito.mock(ReadOnlyTransferDao.class);
		private final TransactionIo transactionIo = new DbTransferIoAdapter(this.transferDao,
				MapperUtils.createDbModelToModelNisMapper(new MockAccountLookup()));

		TransferBlockPair createTransferBlockPair() {
			final DbTransferTransaction dbTransfer = RandomDbTransactionFactory.createTransferWithTimeStamp(123);
			final DbBlock dbBlock = NisUtils.createDbBlockWithTimeStampAtHeight(1, VALID_BLOCK_HEIGHT);
			return new TransferBlockPair(dbTransfer, dbBlock);
		}
	}
}

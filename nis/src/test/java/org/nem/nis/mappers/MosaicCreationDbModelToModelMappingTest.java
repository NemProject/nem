package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

import java.util.Collections;

public class MosaicCreationDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<DbMosaicCreationTransaction, MosaicCreationTransaction> {
	private static final Mosaic MOSAIC = Mockito.mock(Mosaic.class);
	private static final DbMosaic DB_MOSAIC = Mockito.mock(DbMosaic.class);

	@Test
	public void dbTransactionCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicCreationTransaction dbTransaction = new DbMosaicCreationTransaction();
		dbTransaction.setMosaics(Collections.singletonList(DB_MOSAIC));
		dbTransaction.setTimeStamp(1234);
		dbTransaction.setSender(context.dbSender);
		dbTransaction.setDeadline(4321);
		dbTransaction.setFee(123L);

		// Act:
		final MosaicCreationTransaction model = context.mapping.map(dbTransaction);

		// Assert:
		Assert.assertThat(model.getMosaic(), IsEqual.equalTo(MOSAIC));
		Mockito.verify(context.mapper, Mockito.times(1)).map(DB_MOSAIC, Mosaic.class);
	}

	@Override
	protected DbMosaicCreationTransaction createDbModel() {
		final DbMosaicCreationTransaction dbTransaction = new DbMosaicCreationTransaction();
		dbTransaction.setMosaics(Collections.singletonList(DB_MOSAIC));
		return dbTransaction;
	}

	@Override
	protected IMapping<DbMosaicCreationTransaction, MosaicCreationTransaction> createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(DB_MOSAIC, Mosaic.class)).thenReturn(MOSAIC);
		Mockito.when(MOSAIC.getCreator()).then(invocationOnMock -> mapper.map(new DbAccount(), Account.class));
		return new MosaicCreationDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final MosaicCreationDbModelToModelMapping mapping = new MosaicCreationDbModelToModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(DB_MOSAIC, Mosaic.class)).thenReturn(MOSAIC);
			Mockito.when(MOSAIC.getCreator()).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
		}
	}
}

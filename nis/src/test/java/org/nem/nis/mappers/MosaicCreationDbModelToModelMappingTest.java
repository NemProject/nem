package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class MosaicCreationDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<DbMosaicCreationTransaction, MosaicCreationTransaction> {

	@Test
	public void dbTransactionCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicCreationTransaction dbTransaction = new DbMosaicCreationTransaction();
		dbTransaction.setMosaic(context.dbMosaic);
		dbTransaction.setTimeStamp(1234);
		dbTransaction.setSender(context.dbSender);
		dbTransaction.setDeadline(4321);
		dbTransaction.setFee(123L);

		// Act:
		final MosaicCreationTransaction model = context.mapping.map(dbTransaction);

		// Assert:
		Assert.assertThat(model.getMosaic(), IsEqual.equalTo(context.mosaic));
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbMosaic, Mosaic.class);
	}

	@Override
	protected DbMosaicCreationTransaction createDbModel() {
		final DbAccount dbAccount = new DbAccount(Utils.generateRandomAddressWithPublicKey());
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setCreator(dbAccount);

		final DbMosaicCreationTransaction dbTransaction = new DbMosaicCreationTransaction();
		dbTransaction.setSender(dbAccount);
		dbTransaction.setMosaic(dbMosaic);
		return dbTransaction;
	}

	@Override
	protected IMapping<DbMosaicCreationTransaction, MosaicCreationTransaction> createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(Mosaic.class)))
				.thenAnswer(invocationOnMock -> {
					final DbMosaic dbMosaic = ((DbMosaic)invocationOnMock.getArguments()[0]);
					final Mosaic mosaic = Mockito.mock(Mosaic.class);
					final Account creator = mapper.map(dbMosaic.getCreator(), Account.class);
					Mockito.when(mosaic.getCreator()).thenReturn(creator);
					return mosaic;
				});
		return new MosaicCreationDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final Mosaic mosaic = Mockito.mock(Mosaic.class);
		private final DbMosaic dbMosaic = new DbMosaic();
		private final MosaicCreationDbModelToModelMapping mapping = new MosaicCreationDbModelToModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.dbMosaic, Mosaic.class)).thenReturn(this.mosaic);
			Mockito.when(this.mosaic.getCreator()).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
		}
	}
}

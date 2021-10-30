package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class MosaicDefinitionCreationDbModelToModelMappingTest
		extends
			AbstractTransferDbModelToModelMappingTest<DbMosaicDefinitionCreationTransaction, MosaicDefinitionCreationTransaction> {

	@Test
	public void dbTransactionCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinitionCreationTransaction dbTransaction = new DbMosaicDefinitionCreationTransaction();
		dbTransaction.setMosaicDefinition(context.dbMosaicDefinition);
		dbTransaction.setCreationFeeSink(context.dbCreationFeeSink);
		dbTransaction.setCreationFee(25_000_000L);
		dbTransaction.setTimeStamp(1234);
		dbTransaction.setSender(context.dbSender);
		dbTransaction.setDeadline(4321);
		dbTransaction.setFee(123L);

		// Act:
		final MosaicDefinitionCreationTransaction model = context.mapping.map(dbTransaction);

		// Assert:
		MatcherAssert.assertThat(model.getMosaicDefinition(), IsEqual.equalTo(context.mosaicDefinition));
		MatcherAssert.assertThat(model.getCreationFeeSink(), IsEqual.equalTo(context.creationFeeSink));
		MatcherAssert.assertThat(model.getCreationFee(), IsEqual.equalTo(Amount.fromNem(25)));
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbMosaicDefinition, MosaicDefinition.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbCreationFeeSink, Account.class);
	}

	@Override
	protected DbMosaicDefinitionCreationTransaction createDbModel() {
		final DbAccount dbAccount = new DbAccount(Utils.generateRandomAddressWithPublicKey());
		final DbAccount dbCreationFeeSink = new DbAccount(Utils.generateRandomAddressWithPublicKey());
		final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		dbMosaicDefinition.setCreator(dbAccount);

		final DbMosaicDefinitionCreationTransaction dbTransaction = new DbMosaicDefinitionCreationTransaction();
		dbTransaction.setMosaicDefinition(dbMosaicDefinition);
		dbTransaction.setSender(dbAccount);
		dbTransaction.setCreationFeeSink(dbCreationFeeSink);
		dbTransaction.setCreationFee(25_000_000L);
		return dbTransaction;
	}

	@Override
	protected IMapping<DbMosaicDefinitionCreationTransaction, MosaicDefinitionCreationTransaction> createMapping(final IMapper mapper) {
		// map the db mosaic to a non-null model mosaic with the same creator as the db mosaic
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(MosaicDefinition.class))).thenAnswer(invocationOnMock -> {
			final DbMosaicDefinition dbMosaicDefinition = ((DbMosaicDefinition) invocationOnMock.getArguments()[0]);
			final MosaicDefinition mosaicDefinition = Mockito.mock(MosaicDefinition.class);
			final Account creator = mapper.map(dbMosaicDefinition.getCreator(), Account.class);
			Mockito.when(mosaicDefinition.getCreator()).thenReturn(creator);
			return mosaicDefinition;
		});
		return new MosaicDefinitionCreationDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final MosaicDefinition mosaicDefinition = Mockito.mock(MosaicDefinition.class);
		private final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		private final Account creationFeeSink = Utils.generateRandomAccount();
		private final DbAccount dbCreationFeeSink = Mockito.mock(DbAccount.class);
		private final MosaicDefinitionCreationDbModelToModelMapping mapping = new MosaicDefinitionCreationDbModelToModelMapping(
				this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.dbMosaicDefinition, MosaicDefinition.class)).thenReturn(this.mosaicDefinition);
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbCreationFeeSink, Account.class)).thenReturn(this.creationFeeSink);

			// the mosaic must have a matching creator
			Mockito.when(this.mosaicDefinition.getCreator()).thenReturn(this.sender);
		}
	}
}

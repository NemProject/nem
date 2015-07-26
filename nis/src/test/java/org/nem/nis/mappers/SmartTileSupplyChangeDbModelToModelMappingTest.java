package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class SmartTileSupplyChangeDbModelToModelMappingTest extends AbstractTransferDbModelToModelMappingTest<DbSmartTileSupplyChangeTransaction, SmartTileSupplyChangeTransaction> {

	@Test
	public void dbTransactionCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbSmartTileSupplyChangeTransaction dbTransaction = new DbSmartTileSupplyChangeTransaction();
		dbTransaction.setTimeStamp(1234);
		dbTransaction.setDeadline(4321);
		dbTransaction.setSender(context.dbSender);
		dbTransaction.setFee(123L);
		dbTransaction.setDbMosaicId(234L);
		dbTransaction.setSupplyType(1);
		dbTransaction.setQuantity(123L);

		// Act:
		final SmartTileSupplyChangeTransaction model = context.mapping.map(dbTransaction);

		// Assert:
		Assert.assertThat(model.getMosaicId(), IsEqual.equalTo(context.mosaicId));
		Assert.assertThat(model.getSupplyType(), IsEqual.equalTo(SmartTileSupplyType.CreateSmartTiles));
		Assert.assertThat(model.getDelta(), IsEqual.equalTo(Supply.fromValue(123)));

		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbMosaicId, MosaicId.class);
	}

	@Override
	protected DbSmartTileSupplyChangeTransaction createDbModel() {
		final DbSmartTileSupplyChangeTransaction dbTransaction = new DbSmartTileSupplyChangeTransaction();
		dbTransaction.setDbMosaicId(234L);
		dbTransaction.setSupplyType(1);
		dbTransaction.setQuantity(123L);
		return dbTransaction;
	}

	@Override
	protected IMapping<DbSmartTileSupplyChangeTransaction, SmartTileSupplyChangeTransaction> createMapping(final IMapper mapper) {
		final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.food"), "apples");
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(MosaicId.class))).thenReturn(mosaicId);
		return new SmartTileSupplyChangeDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.food"), "apples");
		private final DbMosaicId dbMosaicId = new DbMosaicId(234L);
		private final SmartTileSupplyChangeDbModelToModelMapping mapping = new SmartTileSupplyChangeDbModelToModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbMosaicId, MosaicId.class)).thenReturn(this.mosaicId);
		}
	}
}

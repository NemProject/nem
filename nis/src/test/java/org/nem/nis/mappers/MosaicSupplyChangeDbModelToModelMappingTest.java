package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class MosaicSupplyChangeDbModelToModelMappingTest
		extends
			AbstractTransferDbModelToModelMappingTest<DbMosaicSupplyChangeTransaction, MosaicSupplyChangeTransaction> {

	@Test
	public void dbTransactionCanBeMappedToModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicSupplyChangeTransaction dbTransaction = new DbMosaicSupplyChangeTransaction();
		dbTransaction.setTimeStamp(1234);
		dbTransaction.setDeadline(4321);
		dbTransaction.setSender(context.dbSender);
		dbTransaction.setFee(123L);
		dbTransaction.setDbMosaicId(234L);
		dbTransaction.setSupplyType(1);
		dbTransaction.setQuantity(123L);

		// Act:
		final MosaicSupplyChangeTransaction model = context.mapping.map(dbTransaction);

		// Assert:
		MatcherAssert.assertThat(model.getMosaicId(), IsEqual.equalTo(context.mosaicId));
		MatcherAssert.assertThat(model.getSupplyType(), IsEqual.equalTo(MosaicSupplyType.Create));
		MatcherAssert.assertThat(model.getDelta(), IsEqual.equalTo(Supply.fromValue(123)));

		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbMosaicId, MosaicId.class);
	}

	@Override
	protected DbMosaicSupplyChangeTransaction createDbModel() {
		final DbMosaicSupplyChangeTransaction dbTransaction = new DbMosaicSupplyChangeTransaction();
		dbTransaction.setDbMosaicId(234L);
		dbTransaction.setSupplyType(1);
		dbTransaction.setQuantity(123L);
		return dbTransaction;
	}

	@Override
	protected IMapping<DbMosaicSupplyChangeTransaction, MosaicSupplyChangeTransaction> createMapping(final IMapper mapper) {
		final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.food"), "apples");
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(MosaicId.class))).thenReturn(mosaicId);
		return new MosaicSupplyChangeDbModelToModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Account sender = Utils.generateRandomAccount();
		private final DbAccount dbSender = Mockito.mock(DbAccount.class);
		private final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.food"), "apples");
		private final DbMosaicId dbMosaicId = new DbMosaicId(234L);
		private final MosaicSupplyChangeDbModelToModelMapping mapping = new MosaicSupplyChangeDbModelToModelMapping(this.mapper);

		private TestContext() {
			Mockito.when(this.mapper.map(this.dbSender, Account.class)).thenReturn(this.sender);
			Mockito.when(this.mapper.map(this.dbMosaicId, MosaicId.class)).thenReturn(this.mosaicId);
		}
	}
}

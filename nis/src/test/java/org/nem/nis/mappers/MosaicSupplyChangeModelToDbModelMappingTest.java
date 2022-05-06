package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class MosaicSupplyChangeModelToDbModelMappingTest
		extends
			AbstractTransferModelToDbModelMappingTest<MosaicSupplyChangeTransaction, DbMosaicSupplyChangeTransaction> {

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicSupplyChangeTransaction transfer = context.createModel();

		// Act:
		final DbMosaicSupplyChangeTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(dbModel.getDbMosaicId(), IsEqual.equalTo(234L));
		MatcherAssert.assertThat(dbModel.getSupplyType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(dbModel.getQuantity(), IsEqual.equalTo(123L));

		Mockito.verify(context.mapper, Mockito.times(1)).map(context.mosaicId, DbMosaicId.class);
	}

	@Override
	protected MosaicSupplyChangeTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return RandomTransactionFactory.createMosaicSupplyChangeTransaction(timeStamp, sender);
	}

	@Override
	protected MosaicSupplyChangeModelToDbModelMapping createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbMosaicId.class))).thenReturn(new DbMosaicId(234L));
		return new MosaicSupplyChangeModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.food"), "apples");
		private final DbMosaicId dbMosaicId = new DbMosaicId(234L);
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final MosaicSupplyChangeModelToDbModelMapping mapping = new MosaicSupplyChangeModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.mosaicId, DbMosaicId.class)).thenReturn(this.dbMosaicId);
		}

		public MosaicSupplyChangeTransaction createModel() {
			return new MosaicSupplyChangeTransaction(TimeInstant.ZERO, this.signer, this.mosaicId, MosaicSupplyType.Create,
					Supply.fromValue(123));
		}
	}
}

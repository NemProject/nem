package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class SmartTileSupplyChangeModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<SmartTileSupplyChangeTransaction, DbSmartTileSupplyChangeTransaction> {

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final SmartTileSupplyChangeTransaction transfer = context.createModel();

		// Act:
		final DbSmartTileSupplyChangeTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
		Assert.assertThat(dbModel.getDbMosaicId(), IsEqual.equalTo(234L));
		Assert.assertThat(dbModel.getSupplyType(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getQuantity(), IsEqual.equalTo(123L));

		Mockito.verify(context.mapper, Mockito.times(1)).map(context.mosaicId, DbMosaicId.class);
	}

	@Override
	protected SmartTileSupplyChangeTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return RandomTransactionFactory.createSmartTileSupplyChangeTransaction(timeStamp, sender);
	}

	@Override
	protected SmartTileSupplyChangeModelToDbModelMapping createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbMosaicId.class))).thenReturn(new DbMosaicId(234L));
		return new SmartTileSupplyChangeModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.food"), "apples");
		private final DbMosaicId dbMosaicId = new DbMosaicId(234L);
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final SmartTileSupplyChangeModelToDbModelMapping mapping = new SmartTileSupplyChangeModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(mosaicId, DbMosaicId.class)).thenReturn(this.dbMosaicId);
		}

		public SmartTileSupplyChangeTransaction createModel() {
			return new SmartTileSupplyChangeTransaction(
					TimeInstant.ZERO,
					this.signer,
					this.mosaicId,
					SmartTileSupplyType.CreateSmartTiles,
					Supply.fromValue(123));
		}
	}
}

package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class SmartTileModelToDbModelMappingTest {

	@Test
	public void canMapSmartTileToDbSmartTile() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicTransferPair smartTile = new MosaicTransferPair(context.mosaicId, Quantity.fromValue(123));

		// Act:
		final DbSmartTile dbSmartTile = context.mapping.map(smartTile);

		// Assert:
		Assert.assertThat(dbSmartTile.getDbMosaicId(), IsEqual.equalTo(12L));
		Assert.assertThat(dbSmartTile.getQuantity(), IsEqual.equalTo(123L));

		Mockito.verify(context.mapper, Mockito.times(1)).map(context.mosaicId, DbMosaicId.class);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final MosaicId mosaicId = Utils.createMosaicId(5);
		private final DbMosaicId dbMosaicId = Mockito.mock(DbMosaicId.class);
		private final SmartTileModelToDbModelMapping mapping = new SmartTileModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.mosaicId, DbMosaicId.class)).thenReturn(this.dbMosaicId);
			Mockito.when(this.dbMosaicId.getId()).thenReturn(12L);
		}
	}
}

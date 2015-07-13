package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class SmartTileDbModelToModelMappingTest {

	@Test
	public void canMapSmartTileToDbSmartTile() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbSmartTile dbSmartTile = new DbSmartTile();
		dbSmartTile.setDbMosaicId(5L);
		dbSmartTile.setQuantity(123L);

		// Act:
		final SmartTile smartTile = context.mapping.map(dbSmartTile);

		// Assert:
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(context.mosaicId));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123L)));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final MosaicId mosaicId = Utils.createMosaicId(5);
		private final DbMosaicId dbMosaicId = new DbMosaicId(5L);
		private final SmartTileDbModelToModelMapping mapping = new SmartTileDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbMosaicId, MosaicId.class)).thenReturn(this.mosaicId);
		}
	}
}

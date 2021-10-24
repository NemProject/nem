package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class MosaicModelToDbModelMappingTest {

	@Test
	public void canMapMosaicToDbMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final Mosaic mosaic = new Mosaic(context.mosaicId, Quantity.fromValue(123));

		// Act:
		final DbMosaic dbMosaic = context.mapping.map(mosaic);

		// Assert:
		MatcherAssert.assertThat(dbMosaic.getDbMosaicId(), IsEqual.equalTo(12L));
		MatcherAssert.assertThat(dbMosaic.getQuantity(), IsEqual.equalTo(123L));

		Mockito.verify(context.mapper, Mockito.times(1)).map(context.mosaicId, DbMosaicId.class);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final MosaicId mosaicId = Utils.createMosaicId(5);
		private final DbMosaicId dbMosaicId = Mockito.mock(DbMosaicId.class);
		private final MosaicModelToDbModelMapping mapping = new MosaicModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.mosaicId, DbMosaicId.class)).thenReturn(this.dbMosaicId);
			Mockito.when(this.dbMosaicId.getId()).thenReturn(12L);
		}
	}
}

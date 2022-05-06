package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

public class MosaicDbModelToModelMappingTest {

	@Test
	public void canMapMosaicToDbMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setDbMosaicId(5L);
		dbMosaic.setQuantity(123L);

		// Act:
		final Mosaic mosaic = context.mapping.map(dbMosaic);

		// Assert:
		MatcherAssert.assertThat(mosaic.getMosaicId(), IsEqual.equalTo(context.mosaicId));
		MatcherAssert.assertThat(mosaic.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123L)));

		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbMosaicId, MosaicId.class);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final MosaicId mosaicId = Utils.createMosaicId(5);
		private final DbMosaicId dbMosaicId = new DbMosaicId(5L);
		private final MosaicDbModelToModelMapping mapping = new MosaicDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbMosaicId, MosaicId.class)).thenReturn(this.mosaicId);
		}
	}
}

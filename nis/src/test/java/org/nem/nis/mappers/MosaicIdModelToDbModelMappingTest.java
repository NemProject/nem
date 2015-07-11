package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.cache.ReadOnlyMosaicIdCache;
import org.nem.nis.dbmodel.*;

public class MosaicIdModelToDbModelMappingTest {

	@Test
	public void canMapMosaicIdToDbMosaicId() {
		// Arrange:
		final ReadOnlyMosaicIdCache mosaicidCache = Mockito.mock(ReadOnlyMosaicIdCache.class);
		final MosaicId mosaicId = Mockito.mock(MosaicId.class);
		final DbMosaicId expectedDbMosaicId = Mockito.mock(DbMosaicId.class);
		Mockito.when(mosaicidCache.get(mosaicId)).thenReturn(expectedDbMosaicId);

		final MosaicIdModelToDbModelMapping mapping = new MosaicIdModelToDbModelMapping(mosaicidCache);

		// Act:
		final DbMosaicId dbMosaicId = mapping.map(mosaicId);

		// Assert:
		Assert.assertThat(dbMosaicId, IsEqual.equalTo(expectedDbMosaicId));
		Mockito.verify(mosaicidCache, Mockito.only()).get(mosaicId);
	}
}

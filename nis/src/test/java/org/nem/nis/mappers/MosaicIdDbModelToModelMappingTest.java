package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.cache.ReadOnlyMosaicIdCache;
import org.nem.nis.dbmodel.DbMosaicId;

public class MosaicIdDbModelToModelMappingTest {

	@Test
	public void canMapDbMosaicIdToMosaicId() {
		// Arrange:
		final ReadOnlyMosaicIdCache mosaicidCache = Mockito.mock(ReadOnlyMosaicIdCache.class);
		final MosaicId expectedMosaicId = Mockito.mock(MosaicId.class);
		final DbMosaicId dbMosaicId = Mockito.mock(DbMosaicId.class);
		Mockito.when(mosaicidCache.get(dbMosaicId)).thenReturn(expectedMosaicId);

		final MosaicIdDbModelToModelMapping mapping = new MosaicIdDbModelToModelMapping(mosaicidCache);

		// Act:
		final MosaicId mosaicId = mapping.map(dbMosaicId);

		// Assert:
		Assert.assertThat(mosaicId, IsEqual.equalTo(expectedMosaicId));
		Mockito.verify(mosaicidCache, Mockito.only()).get(dbMosaicId);
	}
}

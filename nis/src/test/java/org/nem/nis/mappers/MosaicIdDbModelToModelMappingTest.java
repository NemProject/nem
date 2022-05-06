package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
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
		final ReadOnlyMosaicIdCache mosaicIdCache = Mockito.mock(ReadOnlyMosaicIdCache.class);
		final MosaicId expectedMosaicId = Mockito.mock(MosaicId.class);
		final DbMosaicId dbMosaicId = Mockito.mock(DbMosaicId.class);
		Mockito.when(mosaicIdCache.get(dbMosaicId)).thenReturn(expectedMosaicId);

		final MosaicIdDbModelToModelMapping mapping = new MosaicIdDbModelToModelMapping(mosaicIdCache);

		// Act:
		final MosaicId mosaicId = mapping.map(dbMosaicId);

		// Assert:
		MatcherAssert.assertThat(mosaicId, IsEqual.equalTo(expectedMosaicId));
		Mockito.verify(mosaicIdCache, Mockito.only()).get(dbMosaicId);
	}
}

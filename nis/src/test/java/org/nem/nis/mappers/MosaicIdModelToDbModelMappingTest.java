package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.nis.cache.ReadOnlyMosaicIdCache;
import org.nem.nis.dbmodel.DbMosaicId;

public class MosaicIdModelToDbModelMappingTest {

	@Test
	public void canMapMosaicIdToDbMosaicId() {
		// Arrange:
		final ReadOnlyMosaicIdCache mosaicIdCache = Mockito.mock(ReadOnlyMosaicIdCache.class);
		final MosaicId mosaicId = Mockito.mock(MosaicId.class);
		final DbMosaicId expectedDbMosaicId = Mockito.mock(DbMosaicId.class);
		Mockito.when(mosaicIdCache.get(mosaicId)).thenReturn(expectedDbMosaicId);

		final MosaicIdModelToDbModelMapping mapping = new MosaicIdModelToDbModelMapping(mosaicIdCache);

		// Act:
		final DbMosaicId dbMosaicId = mapping.map(mosaicId);

		// Assert:
		MatcherAssert.assertThat(dbMosaicId, IsEqual.equalTo(expectedDbMosaicId));
		Mockito.verify(mosaicIdCache, Mockito.only()).get(mosaicId);
	}
}

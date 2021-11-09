package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.test.MapperUtils;

public class NisMapperFactoryTest {

	@Test
	public void canCreateDbModelToModelNisMapper() {
		// Act:
		final NisMapperFactory factory = new NisMapperFactory(MapperUtils.createMapperFactory());
		final NisDbModelToModelMapper mapper = factory.createDbModelToModelNisMapper(Mockito.mock(AccountLookup.class));

		// Assert:
		MatcherAssert.assertThat(mapper, IsNull.notNullValue());
	}
}

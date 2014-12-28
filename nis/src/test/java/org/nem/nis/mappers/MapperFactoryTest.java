package org.nem.nis.mappers;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.AccountLookup;

public class MapperFactoryTest {

	@Test
	public void canCreateModelToDbModelMapper() {
		// Act:
		final IMapper mapper = MapperFactory.createModelToDbModelMapper(Mockito.mock(AccountDaoLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
	}

	@Test
	public void canCreateDbModelToModelMapper() {
		// Act:
		final IMapper mapper = MapperFactory.createDbModelToModelMapper(Mockito.mock(AccountLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
	}
}
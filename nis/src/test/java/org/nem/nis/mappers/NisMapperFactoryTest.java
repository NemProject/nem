package org.nem.nis.mappers;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.AccountLookup;

public class NisMapperFactoryTest {

	@Test
	public void canCreateDbModelToModelNisMapper() {
		// Act:
		final NisMapperFactory factory = new NisMapperFactory(new DefaultMapperFactory());
		final NisDbModelToModelMapper mapper = factory.createDbModelToModelNisMapper(Mockito.mock(AccountLookup.class));

		// Assert:
		Assert.assertThat(mapper, IsNull.notNullValue());
	}
}
package org.nem.nis.harvesting;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.validators.TransactionValidatorFactory;

public class UnconfirmedStateFactoryTest {

	@Test
	public void createCreatesNonNullState() {
		// Arrange:
		final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(
				Mockito.mock(TransactionValidatorFactory.class),
				cache -> Mockito.mock(TransactionObserver.class),
				Mockito.mock(ReadOnlyNisCache.class),
				Mockito.mock(TimeProvider.class),
				() -> BlockHeight.MAX);

		// Act:
		final UnconfirmedState state = factory.create(Mockito.mock(NisCache.class), Mockito.mock(UnconfirmedTransactionsCache.class));

		// Assert:
		Assert.assertThat(state, IsNull.notNullValue());
	}
}
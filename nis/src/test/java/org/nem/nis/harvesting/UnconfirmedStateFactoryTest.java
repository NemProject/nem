package org.nem.nis.harvesting;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.secret.BlockTransactionObserver;
import org.nem.nis.test.NisUtils;

public class UnconfirmedStateFactoryTest {

	@Test
	public void createCreatesNonNullState() {
		// Arrange:
		final UnconfirmedStateFactory factory = new UnconfirmedStateFactory(
				NisUtils.createTransactionValidatorFactory(),
				cache -> Mockito.mock(BlockTransactionObserver.class),
				Mockito.mock(TimeProvider.class),
				() -> BlockHeight.MAX);

		// Act:
		final UnconfirmedState state = factory.create(Mockito.mock(NisCache.class), Mockito.mock(UnconfirmedTransactionsCache.class));

		// Assert:
		Assert.assertThat(state, IsNull.notNullValue());
	}
}
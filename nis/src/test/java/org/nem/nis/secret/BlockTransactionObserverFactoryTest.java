package org.nem.nis.secret;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.AccountAnalyzer;

public class BlockTransactionObserverFactoryTest {

	@Test
	public void createExecuteCommitObserverReturnsValidObserver() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createExecuteCommitObserver(Mockito.mock(AccountAnalyzer.class));

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
	}

	@Test
	public void createUndoCommitObserverReturnsValidObserver() {
		// Arrange:
		final BlockTransactionObserverFactory factory = new BlockTransactionObserverFactory();

		// Act:
		final BlockTransactionObserver observer = factory.createUndoCommitObserver(Mockito.mock(AccountAnalyzer.class));

		// Assert:
		Assert.assertThat(observer, IsNull.notNullValue());
	}
}
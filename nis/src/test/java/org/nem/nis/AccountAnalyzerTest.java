package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.poi.*;

public class AccountAnalyzerTest {

	@Test
	public void analyzerCanBeCreated() {
		// Act:
		final TestContext context = new TestContext();

		// Assert.
		Assert.assertThat(context.analyzer.getAccountCache(), IsSame.sameInstance(context.accountCache));
		Assert.assertThat(context.analyzer.getPoiFacade(), IsSame.sameInstance(context.poiFacade));
	}

	@Test
	public void copyCreatesNewAnalyzerByDelegatingToComponents() {
		// Arrange:
		final AccountCache copyAccountCache = Mockito.mock(AccountCache.class);
		final PoiFacade copyPoiFacade = Mockito.mock(PoiFacade.class);
		final TestContext context = new TestContext();

		Mockito.when(context.accountCache.copy()).thenReturn(copyAccountCache);
		Mockito.when(context.poiFacade.copy()).thenReturn(copyPoiFacade);

		// Act:
		final AccountAnalyzer copy = context.analyzer.copy();

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).copy();
		Mockito.verify(context.poiFacade, Mockito.times(1)).copy();
		Assert.assertThat(copy.getAccountCache(), IsSame.sameInstance(copyAccountCache));
		Assert.assertThat(copy.getPoiFacade(), IsSame.sameInstance(copyPoiFacade));
	}

	@Test
	public void shallowCopyToDelegatesToComponents() {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext targetContext = new TestContext();

		// Act:
		context.analyzer.shallowCopyTo(targetContext.analyzer);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).shallowCopyTo(targetContext.accountCache);
		Mockito.verify(context.poiFacade, Mockito.times(1)).shallowCopyTo(targetContext.poiFacade);
	}

	@Test
	public void removeAccountDelegatesToComponents() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final TestContext context = new TestContext();

		// Act:
		context.analyzer.removeAccount(address);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).removeFromCache(address);
		Mockito.verify(context.poiFacade, Mockito.times(1)).removeFromCache(address);
	}

	private static class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final AccountAnalyzer analyzer = new AccountAnalyzer(this.accountCache, this.poiFacade);
	}
}
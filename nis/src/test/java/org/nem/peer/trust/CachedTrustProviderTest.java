package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.ColumnVector;
import org.nem.core.time.*;

public class CachedTrustProviderTest {

	@Test
	public void trustValuesAreComputedFirstTime() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(0);

		// Act:
		final ColumnVector trustValues = context.computeTrust();

		// Assert:
		Assert.assertThat(trustValues, IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
		context.assertTrustProviderCalls(1);
	}

	@Test
	public void trustValuesAreComputedNotComputedWithinCacheInterval() {
		// Assert:
		assertSingleTrustComputation(10, 10);
		assertSingleTrustComputation(10, 11);
		assertSingleTrustComputation(10, 75);
		assertSingleTrustComputation(10, 110);
	}

	private static void assertSingleTrustComputation(final int time1, final int time2) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(time1);
		context.computeTrust();
		context.setCurrentTime(time2);

		// Act:
		final ColumnVector trustValues = context.computeTrust();

		// Assert:
		Assert.assertThat(trustValues, IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
		context.assertTrustProviderCalls(1);
	}

	@Test
	public void trustValuesAreComputedOutsideOfCacheInterval() {
		// Assert:
		assertTwoTrustComputations(10, 111);
		assertTwoTrustComputations(10, 181);
		assertTwoTrustComputations(10, 333);
	}

	private static void assertTwoTrustComputations(final int time1, final int time2) {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(time1);
		context.computeTrust();
		context.setCurrentTime(time2);

		// Act:
		final ColumnVector trustValues = context.computeTrust();

		// Assert:
		Assert.assertThat(trustValues, IsEqual.equalTo(new ColumnVector(0.25, 0.75)));
		context.assertTrustProviderCalls(2);
	}

	@Test
	public void lastTrustValueComputationIsCached() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(10);
		context.computeTrust();
		context.setCurrentTime(111);
		context.computeTrust();
		context.setCurrentTime(211);

		// Act:
		final ColumnVector trustValues = context.computeTrust();

		// Assert: the value calculated at 111 is cached and returned when querying at 211
		Assert.assertThat(trustValues, IsEqual.equalTo(new ColumnVector(0.25, 0.75)));
		context.assertTrustProviderCalls(2);
	}

	private static class TestContext {
		private final TrustProvider innerTrustProvider = Mockito.mock(TrustProvider.class);
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final CachedTrustProvider trustProvider = new CachedTrustProvider(this.innerTrustProvider, 100, this.timeProvider);

		public TestContext() {
			Mockito.when(this.innerTrustProvider.computeTrust(Mockito.any()))
					.thenReturn(new ColumnVector(1, 1), new ColumnVector(1, 3), new ColumnVector(1, 7));
		}

		public ColumnVector computeTrust() {
			return this.trustProvider.computeTrust(this.context);
		}

		public void setCurrentTime(final int time) {
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(time));
		}

		public void assertTrustProviderCalls(final int numCalls) {
			Mockito.verify(this.innerTrustProvider, Mockito.times(numCalls)).computeTrust(this.context);
		}
	}
}
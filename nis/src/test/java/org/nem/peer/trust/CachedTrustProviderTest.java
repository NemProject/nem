package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.core.time.*;
import org.nem.peer.test.PeerUtils;

public class CachedTrustProviderTest {

	@Test
	public void trustValuesAreComputedFirstTime() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(0);

		// Act:
		final TrustResult result = context.computeTrust();

		// Assert:
		Assert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust1Nodes));
		Assert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
		context.assertTrustProviderCalls(1);
	}

	@Test
	public void trustValuesAreNotComputedWithinCacheInterval() {
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
		final TrustResult result = context.computeTrust();

		// Assert:
		Assert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust1Nodes));
		Assert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
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
		final TrustResult result = context.computeTrust();

		// Assert:
		Assert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust2Nodes));
		Assert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.25, 0.75)));
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
		final TrustResult result = context.computeTrust();

		// Assert: the value calculated at 111 is cached and returned when querying at 211
		Assert.assertThat(result.getTrustContext().getNodes(), IsEqual.equalTo(context.trust2Nodes));
		Assert.assertThat(result.getTrustValues(), IsEqual.equalTo(new ColumnVector(0.25, 0.75)));
		context.assertTrustProviderCalls(2);
	}

	@Test
	public void copyOfTrustValuesIsReturned() {
		// Arrange:
		final TestContext context = new TestContext();
		context.setCurrentTime(0);

		// Act:
		final ColumnVector trustValues1 = context.computeTrust().getTrustValues();
		trustValues1.setAt(0, 0);
		final ColumnVector trustValues2 = context.computeTrust().getTrustValues();

		// Assert:
		Assert.assertThat(trustValues1, IsEqual.equalTo(new ColumnVector(0.0, 0.5)));
		Assert.assertThat(trustValues2, IsEqual.equalTo(new ColumnVector(0.5, 0.5)));
		context.assertTrustProviderCalls(1);
	}

	// TODO 20150404 J-J fix these tests
	private static class TestContext {
		private final TrustProvider innerTrustProvider = Mockito.mock(TrustProvider.class);
		private final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		private final TrustContext context = Mockito.mock(TrustContext.class);
		private final CachedTrustProvider trustProvider = new CachedTrustProvider(this.innerTrustProvider, 100, this.timeProvider);

		private final Node[] trust1Nodes = new Node[] { NodeUtils.createNodeWithName("1a"), NodeUtils.createNodeWithName("1b") };
		private final Node[] trust2Nodes = new Node[] { NodeUtils.createNodeWithName("2c"), NodeUtils.createNodeWithName("2d") };
		private final Node[] trust3Nodes = new Node[] { NodeUtils.createNodeWithName("3e"), NodeUtils.createNodeWithName("3f") };

		public TestContext() {
			final TrustResult result1 = createTrustResult(this.trust1Nodes, new ColumnVector(1, 1));
			final TrustResult result2 = createTrustResult(this.trust2Nodes, new ColumnVector(1, 3));
			final TrustResult result3 = createTrustResult(this.trust3Nodes, new ColumnVector(1, 7));
			Mockito.when(this.innerTrustProvider.computeTrust(Mockito.any())).thenReturn(result1, result2, result3);
		}

		public TrustResult computeTrust() {
			// Act: as a decorator, this provider should not used the passed in context for anything
			return this.trustProvider.computeTrust(this.context);
		}

		public void setCurrentTime(final int time) {
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(time));
		}

		public void assertTrustProviderCalls(final int numCalls) {
			Mockito.verify(this.innerTrustProvider, Mockito.times(numCalls)).computeTrust(this.context);
		}

		private static TrustResult createTrustResult(final Node[] nodes, final ColumnVector vector) {
			final TrustContext context = Mockito.mock(TrustContext.class);
			Mockito.when(context.getNodes()).thenReturn(nodes);
			return new TrustResult(context, vector);
		}
	}
}
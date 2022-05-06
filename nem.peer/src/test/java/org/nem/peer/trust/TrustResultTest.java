package org.nem.peer.trust;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.core.test.ExceptionAssert;
import org.nem.peer.test.PeerUtils;

public class TrustResultTest {

	@Test
	public void canCreateTrustResultWithSameSizeNodesAndTrustValues() {
		// Arrange:
		final ColumnVector trustValues = new ColumnVector(1, 2, 3);
		final Node[] nodes = PeerUtils.createNodeArray(3);
		final TrustContext trustContext = createTrustContext(nodes);

		// Act:
		final TrustResult result = new TrustResult(trustContext, trustValues);

		// Assert:
		MatcherAssert.assertThat(result.getTrustValues(), IsEqual.equalTo(trustValues));
		MatcherAssert.assertThat(result.getTrustContext(), IsEqual.equalTo(trustContext));
	}

	@Test
	public void cannotCreateTrustResultWithFewerNodesThanTrustValues() {
		// Arrange:
		final ColumnVector trustValues = new ColumnVector(1, 2, 3, 4);
		final Node[] nodes = PeerUtils.createNodeArray(3);

		// Act:
		ExceptionAssert.assertThrows(v -> new TrustResult(createTrustContext(nodes), trustValues), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateTrustResultWithMoreNodesThanTrustValues() {
		// Arrange:
		final ColumnVector trustValues = new ColumnVector(1, 2);
		final Node[] nodes = PeerUtils.createNodeArray(3);

		// Act:
		ExceptionAssert.assertThrows(v -> new TrustResult(createTrustContext(nodes), trustValues), IllegalArgumentException.class);
	}

	private static TrustContext createTrustContext(final Node[] nodes) {
		final TrustContext context = Mockito.mock(TrustContext.class);
		Mockito.when(context.getNodes()).thenReturn(nodes);
		return context;
	}
}

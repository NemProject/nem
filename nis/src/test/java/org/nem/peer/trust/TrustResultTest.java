package org.nem.peer.trust;

import junit.framework.TestCase;
import org.hamcrest.core.IsEqual;
import org.junit.*;
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

		// Act:
		final TrustResult result = new TrustResult(nodes, trustValues);

		// Assert:
		Assert.assertThat(result.getTrustValues(), IsEqual.equalTo(trustValues));
		Assert.assertThat(result.getNodes(), IsEqual.equalTo(nodes));
	}

	@Test
	public void cannotCreateTrustResultWithFewerNodesThanTrustValues() {
		// Arrange:
		final ColumnVector trustValues = new ColumnVector(1, 2, 3, 4);
		final Node[] nodes = PeerUtils.createNodeArray(3);

		// Act:
		ExceptionAssert.assertThrows(
				v -> new TrustResult(nodes, trustValues),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateTrustResultWithMoreNodesThanTrustValues() {
		// Arrange:
		final ColumnVector trustValues = new ColumnVector(1, 2);
		final Node[] nodes = PeerUtils.createNodeArray(3);

		// Act:
		ExceptionAssert.assertThrows(
				v -> new TrustResult(nodes, trustValues),
				IllegalArgumentException.class);
	}
}
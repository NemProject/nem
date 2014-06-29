package org.nem.peer.trust;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.peer.node.Node;
import org.nem.peer.test.PeerUtils;

import java.util.*;

import static org.junit.Assert.*;

public class PreTrustAwareNodeSelectorTest {

	@Test
	public void selectNodeDelegatesToWrappedSelector() {
		// Arrange:
		final NodeSelector wrappedSelector = Mockito.mock(NodeSelector.class);
		final Node node = PeerUtils.createNodeWithName("a");
		Mockito.when(wrappedSelector.selectNode()).thenReturn(node);

		final PreTrustAwareNodeSelector selector = new PreTrustAwareNodeSelector(wrappedSelector, null);

		// Act:
		final Node selectedNode = selector.selectNode();

		// Assert:
		Mockito.verify(wrappedSelector, Mockito.times(1)).selectNode();
		Assert.assertThat(selectedNode, IsSame.sameInstance(node));
	}

	@Test
	public void selectNodesDelegatesToWrappedSelector() {
		// Arrange:
		final NodeSelector wrappedSelector = Mockito.mock(NodeSelector.class);
		final List<Node> nodes = new ArrayList<>();
		Mockito.when(wrappedSelector.selectNodes()).thenReturn(nodes);

		final PreTrustAwareNodeSelector selector = new PreTrustAwareNodeSelector(wrappedSelector, null);

		// Act:
		final List<Node> selectedNodes = selector.selectNodes();

		// Assert:
		Mockito.verify(wrappedSelector, Mockito.times(1)).selectNodes();
		Assert.assertThat(selectedNodes, IsSame.sameInstance(nodes));
	}
}
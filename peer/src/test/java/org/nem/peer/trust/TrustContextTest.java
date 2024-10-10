package org.nem.peer.trust;

import java.util.HashSet;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsSame;
import org.junit.*;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.trust.score.NodeExperiences;

public class TrustContextTest {

	@Test
	public void trustContextExposesAllConstructorParameters() {
		// Arrange:
		final Node localNode = NodeUtils.createNodeWithName("bob");
		final Node[] nodes = new Node[]{
				localNode
		};
		final NodeExperiences nodeExperiences = new NodeExperiences();
		final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<>());
		final TrustParameters params = new TrustParameters();

		// Act:
		final TrustContext context = new TrustContext(nodes, localNode, nodeExperiences, preTrustedNodes, params);

		// Assert:
		MatcherAssert.assertThat(context.getNodes(), IsSame.sameInstance(nodes));
		MatcherAssert.assertThat(context.getLocalNode(), IsSame.sameInstance(localNode));
		MatcherAssert.assertThat(context.getNodeExperiences(), IsSame.sameInstance(nodeExperiences));
		MatcherAssert.assertThat(context.getPreTrustedNodes(), IsSame.sameInstance(preTrustedNodes));
		MatcherAssert.assertThat(context.getParams(), IsSame.sameInstance(params));
	}
}

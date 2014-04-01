package org.nem.peer.trust;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.HashSet;

public class TrustContextTest {

	@Test
	public void trustContextExposesAllConstructorParameters() {
		// Arrange:
		final Node localNode = Utils.createNodeWithPort(80);
		final Node[] nodes = new Node[] { localNode };
		final NodeExperiences nodeExperiences = new NodeExperiences();
		final PreTrustedNodes preTrustedNodes = new PreTrustedNodes(new HashSet<Node>());
		final TrustParameters params = new TrustParameters();

		// Act:
		final TrustContext context = new TrustContext(
				nodes,
				localNode,
				nodeExperiences,
				preTrustedNodes,
				params);

		// Assert:
		Assert.assertThat(context.getNodes(), IsSame.sameInstance(nodes));
		Assert.assertThat(context.getLocalNode(), IsSame.sameInstance(localNode));
		Assert.assertThat(context.getNodeExperiences(), IsSame.sameInstance(nodeExperiences));
		Assert.assertThat(context.getPreTrustedNodes(), IsSame.sameInstance(preTrustedNodes));
		Assert.assertThat(context.getParams(), IsSame.sameInstance(params));
	}
}
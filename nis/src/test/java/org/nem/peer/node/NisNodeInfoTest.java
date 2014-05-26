package org.nem.peer.node;

import org.hamcrest.core.*;
import org.hamcrest.core.IsSame;
import org.junit.*;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.peer.test.Utils;

public class NisNodeInfoTest {

	@Test
	public void nodeInfoExposesAllConstructorParameters() {
		// Arrange:
		final Node node = Utils.createNodeWithPort(7);
		final ApplicationMetaData appMetaData = new ApplicationMetaData("nem", "1.0", null, 14);

		// Act:
		final NisNodeInfo nodeInfo = new NisNodeInfo(node, appMetaData);

		// Assert:
		Assert.assertThat(nodeInfo.getNode(), IsSame.sameInstance(node));
		Assert.assertThat(nodeInfo.getAppMetaData(), IsSame.sameInstance(appMetaData));
	}

	@Test
	public void canRoundtripNodeInfoMetaData() {
		// Arrange:
		final Node node = Utils.createNodeWithPort(17);
		final ApplicationMetaData appMetaData = new ApplicationMetaData("nem", "1.0", null, 14);

		// Act:
		final NisNodeInfo nodeInfo = roundtripNodeInfo(new NisNodeInfo(node, appMetaData));

		// Assert:
		Assert.assertThat(nodeInfo.getNode(), IsEqual.equalTo(node));
		Assert.assertThat(nodeInfo.getAppMetaData().getAppName(), IsEqual.equalTo("nem"));
	}

	private static NisNodeInfo roundtripNodeInfo(final NisNodeInfo nodeInfo) {
		return new NisNodeInfo(org.nem.core.test.Utils.roundtripSerializableEntity(nodeInfo, null));
	}
}
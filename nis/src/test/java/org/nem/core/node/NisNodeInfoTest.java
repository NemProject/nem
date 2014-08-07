package org.nem.core.node;

import java.util.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.time.*;

public class NisNodeInfoTest {

	@Test
	public void nodeInfoExposesAllConstructorParameters() {
		// Arrange:
		final Node node = createNodeWithName("a");
		final ApplicationMetaData appMetaData = createAppMetaData("nem", "1.0");

		// Act:
		final NisNodeInfo nodeInfo = new NisNodeInfo(node, appMetaData);

		// Assert:
		Assert.assertThat(nodeInfo.getNode(), IsSame.sameInstance(node));
		Assert.assertThat(nodeInfo.getAppMetaData(), IsSame.sameInstance(appMetaData));
	}

	@Test
	public void canRoundtripNodeInfoMetaData() {
		// Arrange:
		final Node node = createNodeWithName("b");
		final ApplicationMetaData appMetaData = createAppMetaData("nem", "1.0");

		// Act:
		final NisNodeInfo nodeInfo = roundtripNodeInfo(new NisNodeInfo(node, appMetaData));

		// Assert:
		Assert.assertThat(nodeInfo.getNode(), IsEqual.equalTo(node));
		Assert.assertThat(nodeInfo.getAppMetaData().getAppName(), IsEqual.equalTo("nem"));
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final List<Node> nodes = createThreeTestNodes(keyPair);
		final List<ApplicationMetaData> appMetaData = createThreeAppMetaData();

		// Act:
		final NisNodeInfo nodeInfo1 = new NisNodeInfo(nodes.get(0), appMetaData.get(0));
		final NisNodeInfo nodeInfo2 = new NisNodeInfo(nodes.get(0), appMetaData.get(2));
		final NisNodeInfo nodeInfo3 = new NisNodeInfo(nodes.get(2), appMetaData.get(0));
		final NisNodeInfo nodeInfo4 = new NisNodeInfo(nodes.get(0), appMetaData.get(1));
		final NisNodeInfo nodeInfo5 = new NisNodeInfo(nodes.get(1), appMetaData.get(0));

		// Assert:
		Assert.assertThat(nodeInfo1, IsEqual.equalTo(nodeInfo2));
		Assert.assertThat(nodeInfo1, IsEqual.equalTo(nodeInfo3));
		Assert.assertThat(nodeInfo1, IsNot.not(IsEqual.equalTo(nodeInfo4)));
		Assert.assertThat(nodeInfo1, IsNot.not(IsEqual.equalTo(nodeInfo5)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		final KeyPair keyPair = new KeyPair();
		final List<Node> nodes = createThreeTestNodes(keyPair);
		final List<ApplicationMetaData> appMetaData = createThreeAppMetaData();

		// Act:
		final NisNodeInfo nodeInfo1 = new NisNodeInfo(nodes.get(0), appMetaData.get(0));
		final NisNodeInfo nodeInfo2 = new NisNodeInfo(nodes.get(0), appMetaData.get(2));
		final NisNodeInfo nodeInfo3 = new NisNodeInfo(nodes.get(2), appMetaData.get(0));
		final NisNodeInfo nodeInfo4 = new NisNodeInfo(nodes.get(0), appMetaData.get(1));
		final NisNodeInfo nodeInfo5 = new NisNodeInfo(nodes.get(1), appMetaData.get(0));

		// Assert:
		Assert.assertThat(nodeInfo1.hashCode(), IsEqual.equalTo(nodeInfo2.hashCode()));
		Assert.assertThat(nodeInfo1.hashCode(), IsEqual.equalTo(nodeInfo3.hashCode()));
		Assert.assertThat(nodeInfo1.hashCode(), IsNot.not(IsEqual.equalTo(nodeInfo4.hashCode())));
		Assert.assertThat(nodeInfo1.hashCode(), IsNot.not(IsEqual.equalTo(nodeInfo5.hashCode())));
	}

	private static List<Node> createThreeTestNodes(final KeyPair keyPair) {
		return Arrays.asList(
				new Node(new NodeIdentity(keyPair, "a"), NodeEndpoint.fromHost("localhost")),
				new Node(new NodeIdentity(new KeyPair(), "b"), NodeEndpoint.fromHost("localhost")),
				new Node(new NodeIdentity(keyPair, "a"), NodeEndpoint.fromHost("localhost")));
	}

	private static Node createNodeWithName(final String name) {
		return new Node(
				new NodeIdentity(new KeyPair(), name),
				NodeEndpoint.fromHost("localhost"));
	}

	private static NisNodeInfo roundtripNodeInfo(final NisNodeInfo nodeInfo) {
		return new NisNodeInfo(org.nem.core.test.Utils.roundtripSerializableEntity(nodeInfo, null));
	}

	private static List<ApplicationMetaData> createThreeAppMetaData() {
		return Arrays.asList(
				createAppMetaData("nem", "1.0"),
				createAppMetaData("nis", "1.0"),
				createAppMetaData("nem", "1.0"));
	}

	private static ApplicationMetaData createAppMetaData(final String name, final String version) {
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(17));
		return new ApplicationMetaData(name, version, null, timeProvider);
	}
}
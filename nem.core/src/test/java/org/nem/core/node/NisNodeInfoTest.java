package org.nem.core.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.time.*;

import java.util.*;

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

	//region equals / hashCode

	private static Map<String, NisNodeInfo> createNisNodeInfosForEqualityTests(final KeyPair keyPair) {
		final NodeEndpoint endpoint = NodeEndpoint.fromHost("localhost");
		return new HashMap<String, NisNodeInfo>() {
			{
				this.put("default", new NisNodeInfo(new Node(new NodeIdentity(keyPair), endpoint), createAppMetaData("nem", "1.0")));
				this.put("diff-identity", new NisNodeInfo(new Node(new NodeIdentity(new KeyPair()), endpoint), createAppMetaData("nem", "1.0")));
				this.put("diff-metaData", new NisNodeInfo(new Node(new NodeIdentity(new KeyPair()), endpoint), createAppMetaData("nem", "1.1")));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NisNodeInfo info = new NisNodeInfo(
				new Node(new NodeIdentity(keyPair), NodeEndpoint.fromHost("localhost")),
				createAppMetaData("nem", "1.0"));
		final Map<String, NisNodeInfo> infoMap = createNisNodeInfosForEqualityTests(keyPair);

		// Assert:
		Assert.assertThat(infoMap.get("default"), IsEqual.equalTo(info));
		Assert.assertThat(infoMap.get("diff-identity"), IsNot.not(IsEqual.equalTo(info)));
		Assert.assertThat(infoMap.get("diff-metaData"), IsNot.not(IsEqual.equalTo(info)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(info)));
		Assert.assertThat(keyPair, IsNot.not(IsEqual.equalTo((Object)info)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NisNodeInfo info = new NisNodeInfo(
				new Node(new NodeIdentity(keyPair), NodeEndpoint.fromHost("localhost")),
				createAppMetaData("nem", "1.0"));
		final int hashCode = info.hashCode();
		final Map<String, NisNodeInfo> infoMap = createNisNodeInfosForEqualityTests(keyPair);

		// Assert:
		Assert.assertThat(infoMap.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("diff-identity").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-metaData").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	private static Node createNodeWithName(final String name) {
		return new Node(
				new NodeIdentity(new KeyPair(), name),
				NodeEndpoint.fromHost("localhost"));
	}

	private static NisNodeInfo roundtripNodeInfo(final NisNodeInfo nodeInfo) {
		return new NisNodeInfo(org.nem.core.test.Utils.roundtripSerializableEntity(nodeInfo, null));
	}

	private static ApplicationMetaData createAppMetaData(final String name, final String version) {
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(17));
		return new ApplicationMetaData(name, version, null, timeProvider);
	}
}
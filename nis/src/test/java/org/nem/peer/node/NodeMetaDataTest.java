package org.nem.peer.node;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NodeMetaDataTest {

	@Test
	public void metaDataCanBeCreated() {
		// Act:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", "ver");

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("ver"));
	}

	@Test
	public void metaDataCanBeRoundTripped() {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new NodeMetaData("plat", "app", "ver"),
				null);
		final NodeMetaData metaData = new NodeMetaData(deserializer);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("ver"));
	}
}
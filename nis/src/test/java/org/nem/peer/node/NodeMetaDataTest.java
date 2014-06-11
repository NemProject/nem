package org.nem.peer.node;

import org.hamcrest.core.*;
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

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", "ver");

		// Assert:
		Assert.assertThat(new NodeMetaData("plat", "app", "ver"), IsEqual.equalTo(metaData));
		Assert.assertThat(new NodeMetaData(null, "app", "ver"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", null, "ver"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", "app", null), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("p", "app", "ver"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", "a", "ver"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", "app", "v"), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat("plat", IsNot.not(IsEqual.equalTo((Object)metaData)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", "ver");
		final int hashCode = metaData.hashCode();

		// Assert:
		Assert.assertThat(new NodeMetaData("plat", "app", "ver").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeMetaData(null, "app", "ver").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", null, "ver").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", "app", null).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("p", "app", "ver").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", "a", "ver").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", "app", "v").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion
}
package org.nem.peer.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class NodeMetaDataTest {

	@Test
	public void metaDataCanBeCreated() {
		// Act:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", new NodeVersion(3, 0, 0));

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(3, 0, 0)));
	}

	@Test
	public void metaDataCanBeCreatedWithoutVersion() {
		// Act:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", null);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(NodeVersion.ZERO));
	}

	@Test
	public void metaDataCanBeRoundTripped() {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new NodeMetaData("plat", "app", new NodeVersion(3, 0, 0)),
				null);
		final NodeMetaData metaData = new NodeMetaData(deserializer);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(3, 0, 0)));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0));

		// Assert:
		Assert.assertThat(new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0)), IsEqual.equalTo(metaData));
		Assert.assertThat(new NodeMetaData(null, "app", new NodeVersion(1, 0, 0)), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", null, new NodeVersion(1, 0, 0)), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", "app", null), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("p", "app", new NodeVersion(1, 0, 0)), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", "a", new NodeVersion(1, 0, 0)), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(new NodeMetaData("plat", "app", new NodeVersion(2, 0, 0)), IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat("plat", IsNot.not(IsEqual.equalTo((Object)metaData)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0));
		final int hashCode = metaData.hashCode();

		// Assert:
		Assert.assertThat(new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0)).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeMetaData(null, "app", new NodeVersion(1, 0, 0)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", null, new NodeVersion(1, 0, 0)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", "app", null).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("p", "app", new NodeVersion(1, 0, 0)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", "a", new NodeVersion(1, 0, 0)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new NodeMetaData("plat", "app", new NodeVersion(2, 0, 0)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion
}
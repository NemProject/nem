package org.nem.core.node;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.util.*;

public class NodeMetaDataTest {

	//region construction

	@Test
	public void metaDataCanBeCreated() {
		// Act:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", new NodeVersion(3, 0, 0), 4, 7);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(3, 0, 0)));
		Assert.assertThat(metaData.getNetworkId(), IsEqual.equalTo(4));
		Assert.assertThat(metaData.getFeaturesBitmask(), IsEqual.equalTo(7));
	}

	@Test
	public void metaDataCanBeCreatedWithoutVersionAndBitmask() {
		// Act:
		final NodeMetaData metaData = new NodeMetaData("plat", "app");

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(NodeVersion.ZERO));
		Assert.assertThat(metaData.getNetworkId(), IsEqual.equalTo(0));
		Assert.assertThat(metaData.getFeaturesBitmask(), IsEqual.equalTo(0));
	}

	//endregion

	//region serialization

	@Test
	public void metaDataCanBeDeserializedWithAllParameters() {
		// Act:
		final NodeMetaData metaData = createMetaDataFromJson("plat", "app", "3.0.0", 4, 7);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(3, 0, 0)));
		Assert.assertThat(metaData.getNetworkId(), IsEqual.equalTo(4));
		Assert.assertThat(metaData.getFeaturesBitmask(), IsEqual.equalTo(7));
	}

	@Test
	public void metaDataCanBeDeserializedWithoutOptionalParameters() {
		// Act:
		final NodeMetaData metaData = createMetaDataFromJson("plat", "app", "3.0.0", null, null);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(3, 0, 0)));
		Assert.assertThat(metaData.getNetworkId(), IsEqual.equalTo(0));
		Assert.assertThat(metaData.getFeaturesBitmask(), IsEqual.equalTo(0));
	}

	@Test
	public void metaDataCanBeDeserializedWithOnlyVersion() {
		// Act:
		final NodeMetaData metaData = createMetaDataFromJson(null, null, "3.0.0", null, null);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsNull.nullValue());
		Assert.assertThat(metaData.getApplication(), IsNull.nullValue());
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(3, 0, 0)));
		Assert.assertThat(metaData.getNetworkId(), IsEqual.equalTo(0));
		Assert.assertThat(metaData.getFeaturesBitmask(), IsEqual.equalTo(0));
	}

	@Test(expected = MissingRequiredPropertyException.class)
	public void metaDataCannotBeDeserializedWithoutVersion() {
		// Act:
		createMetaDataFromJson("plat", "app", null, 4, 7);
	}

	private static NodeMetaData createMetaDataFromJson(
			final String platform,
			final String application,
			final String version,
			final Integer networkId,
			final Integer features) {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("platform", platform);
		jsonObject.put("application", application);
		jsonObject.put("version", version);
		jsonObject.put("networkId", networkId);
		jsonObject.put("features", features);
		return new NodeMetaData(Utils.createDeserializer(jsonObject));
	}

	@Test
	public void metaDataCanBeRoundTripped() {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new NodeMetaData("plat", "app", new NodeVersion(3, 0, 0), 4, 7),
				null);
		final NodeMetaData metaData = new NodeMetaData(deserializer);

		// Assert:
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo("plat"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("app"));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(3, 0, 0)));
		Assert.assertThat(metaData.getNetworkId(), IsEqual.equalTo(4));
		Assert.assertThat(metaData.getFeaturesBitmask(), IsEqual.equalTo(7));
	}

	//endregion

	//region equals / hashCode

	private static final Map<String, NodeMetaData> DESC_TO_META_DATA_MAP = new HashMap<String, NodeMetaData>() {
		{
			this.put("default", new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0), 4, 7));

			this.put("no-platform", new NodeMetaData(null, "app", new NodeVersion(1, 0, 0), 4, 7));
			this.put("no-app", new NodeMetaData("plat", null, new NodeVersion(1, 0, 0), 4, 7));
			this.put("no-version", new NodeMetaData("plat", "app", null, 4, 7));
			this.put("no-network-id", new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0), 0, 7));
			this.put("no-features", new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0), 4, 0));

			this.put("diff-platform", new NodeMetaData("plat2", "app", new NodeVersion(1, 0, 0), 4, 7));
			this.put("diff-app", new NodeMetaData("plat", "app2", new NodeVersion(1, 0, 0), 4, 7));
			this.put("diff-version", new NodeMetaData("plat", "app", new NodeVersion(2, 0, 0), 4, 7));
			this.put("diff-network-id", new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0), 5, 7));
			this.put("diff-features", new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0), 4, 6));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeMetaData metaData = new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0), 4, 7);

		// Assert:
		Assert.assertThat(DESC_TO_META_DATA_MAP.get("default"), IsEqual.equalTo(metaData));
		for (final Map.Entry<String, NodeMetaData> entry : DESC_TO_META_DATA_MAP.entrySet()) {
			if ("default".equals(entry.getKey())) {
				continue;
			}

			Assert.assertThat(entry.getValue(), IsNot.not(IsEqual.equalTo(metaData)));
		}

		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(metaData)));
		Assert.assertThat("plat", IsNot.not(IsEqual.equalTo((Object)metaData)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new NodeMetaData("plat", "app", new NodeVersion(1, 0, 0), 4, 7).hashCode();

		// Assert:
		Assert.assertThat(DESC_TO_META_DATA_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		for (final Map.Entry<String, NodeMetaData> entry : DESC_TO_META_DATA_MAP.entrySet()) {
			if ("default".equals(entry.getKey())) {
				continue;
			}

			Assert.assertThat(entry.getValue().hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		}
	}

	//endregion
}
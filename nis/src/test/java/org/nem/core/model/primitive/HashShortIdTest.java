package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class HashShortIdTest {

	//region constructor

	@Test
	public void canBeCreated() {
		// Act:
		final HashShortId shortId = new HashShortId(1234L);

		// Assert:
		Assert.assertThat(shortId.getValue(), IsEqual.equalTo(1234L));
	}

	//endregion

	//region serialization

	@Test
	public void hashShortIdCanBeSerialized() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final HashShortId shortId = new HashShortId(123L);

		// Act:
		shortId.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		Assert.assertThat(jsonObject.get("hashShortId"), IsEqual.equalTo(123L));
	}

	@Test
	public void hashShortIdCanBeRoundTripped() {
		// Act:
		final HashShortId height = createRoundTrippedHashShortId(new HashShortId(123L));

		// Assert:
		Assert.assertThat(height.getRaw(), IsEqual.equalTo(123L));
	}

	private static HashShortId createRoundTrippedHashShortId(final HashShortId originalHasShortId) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalHasShortId, null);
		return new HashShortId(deserializer);
	}

	//endregion
}

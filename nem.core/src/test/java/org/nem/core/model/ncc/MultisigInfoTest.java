package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class MultisigInfoTest {

	@Test
	public void infoCanBeCreated() {
		// Act:
		final MultisigInfo info = new MultisigInfo(12, 7);

		// Assert:
		Assert.assertThat(info.getCosignatoriesCount(), IsEqual.equalTo(12));
		Assert.assertThat(info.getMinCosignatories(), IsEqual.equalTo(7));
	}

	@Test
	public void infoCanBeSerialized() {
		// Arrange:
		final MultisigInfo info = new MultisigInfo(12, 7);

		// Act:
		final JsonSerializer serializer = new JsonSerializer(true);
		info.serialize(serializer);
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Assert:
		Assert.assertThat(deserializer.readInt("cosignatoriesCount"), IsEqual.equalTo(12));
		Assert.assertThat(deserializer.readInt("minCosignatories"), IsEqual.equalTo(7));

		// 2 "real" properties and 1 "hidden" (ordering) property
		final int expectedProperties = 2 + 1;
		Assert.assertThat(serializer.getObject().size(), IsEqual.equalTo(expectedProperties));
	}

	@Test
	public void infoCanBeRoundTripped() {
		// Arrange:
		final MultisigInfo originalInfo = new MultisigInfo(12, 7);

		// Act:
		final MultisigInfo info = new MultisigInfo(Utils.roundtripSerializableEntity(originalInfo, null));

		// Assert:
		Assert.assertThat(info.getCosignatoriesCount(), IsEqual.equalTo(12));
		Assert.assertThat(info.getMinCosignatories(), IsEqual.equalTo(7));
	}
}
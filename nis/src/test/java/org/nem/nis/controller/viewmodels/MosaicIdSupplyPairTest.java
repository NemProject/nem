package org.nem.nis.controller.viewmodels;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class MosaicIdSupplyPairTest {

	// region ctor

	@Test
	public void canCreatePair() {
		// Arrange:
		final MosaicId mosaicId = Utils.createMosaicId(5);
		final MosaicIdSupplyPair pair = new MosaicIdSupplyPair(mosaicId, Supply.fromValue(12345));

		// Assert:
		Assert.assertThat(pair.getMosaicId(), IsEqual.equalTo(mosaicId));
		Assert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(12345)));
	}

	@Test
	public void canCreatePairWithMissingParameter() {
		// Arrange:
		final MosaicId mosaicId = Utils.createMosaicId(5);

		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicIdSupplyPair(null, Supply.fromValue(123)), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new MosaicIdSupplyPair(mosaicId, null), IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripPair() {
		// Arrange:
		final MosaicId mosaicId = Utils.createMosaicId(5);
		final MosaicIdSupplyPair original = new MosaicIdSupplyPair(mosaicId, Supply.fromValue(12345));
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);

		// Act:
		final MosaicIdSupplyPair pair = new MosaicIdSupplyPair(deserializer);

		// Assert:
		Assert.assertThat(pair.getMosaicId(), IsEqual.equalTo(mosaicId));
		Assert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(12345)));
	}

	@Test
	public void cannotDeserializeWithMissingRequiredParameter() {
		assertCannotDeserialize("mosaicId");
		assertCannotDeserialize("supply");
	}

	private void assertCannotDeserialize(final String keyToRemove) {
		final MosaicId mosaicId = Utils.createMosaicId(5);
		final MosaicIdSupplyPair pair = new MosaicIdSupplyPair(mosaicId, Supply.fromValue(12345));
		final JsonSerializer serializer = new JsonSerializer();
		pair.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();
		jsonObject.remove(keyToRemove);

		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicIdSupplyPair(new JsonDeserializer(jsonObject, null)), MissingRequiredPropertyException.class);
	}

	// endregion
}

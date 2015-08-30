package org.nem.nis.controller.viewmodels;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class MosaicIdSupplyPairTest {

	// region ctor

	@Test
	public void canCreatePair() {
		// Arrange:
		final MosaicIdSupplyPair pair = new MosaicIdSupplyPair(Utils.createMosaicId(5), Supply.fromValue(12345));

		// Assert:
		Assert.assertThat(pair.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(5)));
		Assert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(12345)));
	}

	@Test
	public void cannotCreatePairWithMissingParameter() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicIdSupplyPair(null, Supply.fromValue(123)), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new MosaicIdSupplyPair(Utils.createMosaicId(5), null), IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canSerializePair() {
		// Arrange:
		final MosaicIdSupplyPair pair = new MosaicIdSupplyPair(Utils.createMosaicId(5), Supply.fromValue(12345));

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(pair);

		// Assert:
		final JSONObject mosaicIdJsonObject = (JSONObject)jsonObject.get("mosaicId");
		Assert.assertThat(mosaicIdJsonObject.get("namespaceId"), IsEqual.equalTo("id5"));
		Assert.assertThat(mosaicIdJsonObject.get("name"), IsEqual.equalTo("5"));
		Assert.assertThat(jsonObject.get("supply"), IsEqual.equalTo(12345L));
	}

	@Test
	public void canRoundTripPair() {
		// Arrange:
		final MosaicIdSupplyPair original = new MosaicIdSupplyPair(Utils.createMosaicId(5), Supply.fromValue(12345));
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);

		// Act:
		final MosaicIdSupplyPair pair = new MosaicIdSupplyPair(deserializer);

		// Assert:
		Assert.assertThat(pair.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(5)));
		Assert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(12345)));
	}

	@Test
	public void cannotDeserializeWithMissingRequiredParameter() {
		assertCannotDeserialize("mosaicId");
		assertCannotDeserialize("supply");
	}

	private static void assertCannotDeserialize(final String keyToRemove) {
		final MosaicIdSupplyPair pair = new MosaicIdSupplyPair(Utils.createMosaicId(5), Supply.fromValue(12345));
		final JSONObject jsonObject = JsonSerializer.serializeToJson(pair);
		jsonObject.remove(keyToRemove);

		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicIdSupplyPair(new JsonDeserializer(jsonObject, null)), MissingRequiredPropertyException.class);
	}

	// endregion
}

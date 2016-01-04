package org.nem.core.model.ncc;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

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
		Assert.assertThat(mosaicIdJsonObject.get("name"), IsEqual.equalTo("name5"));
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

	//region equals / hashCode

	private static Map<String, MosaicIdSupplyPair> createMosaicIdSupplyPairsForEqualityTests() {
		return new HashMap<String, MosaicIdSupplyPair>() {
			{
				this.put("default", createMosaicIdSupplyPair(5, 123));
				this.put("diff-mosaicId", createMosaicIdSupplyPair(7, 123));
				this.put("diff-supply", createMosaicIdSupplyPair(5, 234));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicIdSupplyPair pair = createMosaicIdSupplyPair(5, 123);

		// Assert:
		for (final Map.Entry<String, MosaicIdSupplyPair> entry : createMosaicIdSupplyPairsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					!entry.getKey().equals("default") ? IsNot.not(IsEqual.equalTo(pair)) : IsEqual.equalTo(pair));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(pair)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(pair)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = createMosaicIdSupplyPair(5, 123).hashCode();

		// Assert:
		for (final Map.Entry<String, MosaicIdSupplyPair> entry : createMosaicIdSupplyPairsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					!entry.getKey().equals("default") ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static MosaicIdSupplyPair createMosaicIdSupplyPair(final int id, final long supply) {
		return new MosaicIdSupplyPair(Utils.createMosaicId(id), Supply.fromValue(supply));
	}

	// endregion
}

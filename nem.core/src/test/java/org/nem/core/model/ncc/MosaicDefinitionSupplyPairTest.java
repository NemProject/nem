package org.nem.core.model.ncc;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class MosaicDefinitionSupplyPairTest {

	// region ctor

	@Test
	public void canCreatePair() {
		// Arrange:
		final MosaicDefinitionSupplyPair pair = new MosaicDefinitionSupplyPair(Utils.createMosaicDefinition(5), Supply.fromValue(12345));

		// Assert:
		Assert.assertThat(pair.getMosaicDefinition(), IsEqual.equalTo(Utils.createMosaicDefinition(5)));
		Assert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(12345)));
	}

	@Test
	public void cannotCreatePairWithMissingParameter() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicDefinitionSupplyPair(null, Supply.fromValue(123)), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new MosaicDefinitionSupplyPair(Utils.createMosaicDefinition(5), null), IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canSerializePair() {
		// Arrange:
		final MosaicDefinitionSupplyPair pair = new MosaicDefinitionSupplyPair(Utils.createMosaicDefinition(5), Supply.fromValue(12345));

		// Act:
		final JSONObject jsonObject = JsonSerializer.serializeToJson(pair);

		// Assert:
		final JSONObject mosaicDefinitionJsonObject = (JSONObject)jsonObject.get("mosaicDefinition");
		final JSONObject mosaicIdJsonObject = (JSONObject)mosaicDefinitionJsonObject.get("id");
		Assert.assertThat(mosaicIdJsonObject.get("namespaceId"), IsEqual.equalTo("id5"));
		Assert.assertThat(mosaicIdJsonObject.get("name"), IsEqual.equalTo("name5"));
		Assert.assertThat(jsonObject.get("supply"), IsEqual.equalTo(12345L));
	}

	@Test
	public void canRoundTripPair() {
		// Arrange:
		final MosaicDefinitionSupplyPair original = new MosaicDefinitionSupplyPair(Utils.createMosaicDefinition(5), Supply.fromValue(12345));
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, new MockAccountLookup());

		// Act:
		final MosaicDefinitionSupplyPair pair = new MosaicDefinitionSupplyPair(deserializer);

		// Assert:
		Assert.assertThat(pair.getMosaicDefinition(), IsEqual.equalTo(Utils.createMosaicDefinition(5)));
		Assert.assertThat(pair.getSupply(), IsEqual.equalTo(Supply.fromValue(12345)));
	}

	@Test
	public void cannotDeserializeWithMissingRequiredParameter() {
		assertCannotDeserialize("mosaicDefinition");
		assertCannotDeserialize("supply");
	}

	private static void assertCannotDeserialize(final String keyToRemove) {
		final MosaicDefinitionSupplyPair pair = new MosaicDefinitionSupplyPair(Utils.createMosaicDefinition(5), Supply.fromValue(12345));
		final JSONObject jsonObject = JsonSerializer.serializeToJson(pair);
		jsonObject.remove(keyToRemove);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> new MosaicDefinitionSupplyPair(Utils.createDeserializer(jsonObject)),
				MissingRequiredPropertyException.class);
	}

	// endregion

	//region equals / hashCode

	private static Map<String, MosaicDefinitionSupplyPair> createMosaicDefinitionSupplyPairsForEqualityTests() {
		return new HashMap<String, MosaicDefinitionSupplyPair>() {
			{
				this.put("default", createMosaicDefinitionSupplyPair(5, 123));
				this.put("diff-mosaicDefinition", createMosaicDefinitionSupplyPair(7, 123));
				this.put("diff-supply", createMosaicDefinitionSupplyPair(5, 234));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicDefinitionSupplyPair pair = createMosaicDefinitionSupplyPair(5, 123);

		// Assert:
		for (final Map.Entry<String, MosaicDefinitionSupplyPair> entry : createMosaicDefinitionSupplyPairsForEqualityTests().entrySet()) {
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
		final int hashCode = createMosaicDefinitionSupplyPair(5, 123).hashCode();

		// Assert:
		for (final Map.Entry<String, MosaicDefinitionSupplyPair> entry : createMosaicDefinitionSupplyPairsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					!entry.getKey().equals("default") ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static MosaicDefinitionSupplyPair createMosaicDefinitionSupplyPair(final int id, final long supply) {
		return new MosaicDefinitionSupplyPair(Utils.createMosaicDefinition(id), Supply.fromValue(supply));
	}

	// endregion
}

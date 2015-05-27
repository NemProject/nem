package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.function.Consumer;

public class MultisigMinCosignatoriesModificationTest {

	// region creation

	@Test
	public void canCreateMultisigMinCosignatoriesModificationWithPositiveMinCosignatories() {
		// Act:
		final MultisigMinCosignatoriesModification modification = new MultisigMinCosignatoriesModification(MultisigModificationType.MinCosignatories, 12);

		// Assert:
		Assert.assertThat(modification.getMinCosignatories(), IsEqual.equalTo(12));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(MultisigModificationType.MinCosignatories));
	}

	@Test
	public void canCreateMultisigMinCosignatoriesModificationWithZeroMinCosignatories() {
		// Act:
		final MultisigMinCosignatoriesModification modification = new MultisigMinCosignatoriesModification(MultisigModificationType.MinCosignatories, 0);

		// Assert:
		Assert.assertThat(modification.getMinCosignatories(), IsEqual.equalTo(0));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(MultisigModificationType.MinCosignatories));
	}

	@Test
	public void cannotCreateMultisigMinCosignatoriesModificationWithWrongType() {
		ExceptionAssert.assertThrows(
				v -> new MultisigMinCosignatoriesModification(MultisigModificationType.Unknown, 12),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new MultisigMinCosignatoriesModification(MultisigModificationType.AddCosignatory, 12),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new MultisigMinCosignatoriesModification(MultisigModificationType.DelCosignatory, 12),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMultisigMinCosignatoriesModificationWithNegativeMinCosignatories() {
		ExceptionAssert.assertThrows(
				v -> new MultisigMinCosignatoriesModification(MultisigModificationType.MinCosignatories, -1),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new MultisigMinCosignatoriesModification(MultisigModificationType.MinCosignatories, -10),
				IllegalArgumentException.class);
	}

	// endregion

	// region deserialization

	@Test
	public void cannotDeserializeWhenMinCosignatoriesIsMissing() {
		this.assertDeserializationFailure(jsonObject -> jsonObject.remove("minCosignatories"), SerializationException.class);
	}

	@Test
	public void cannotDeserializeWhenMinCosignatoriesIsInvalid() {
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("minCosignatories", -1), IllegalArgumentException.class);
	}

	@Test
	public void cannotDeserializeWhenModificationTypeIsMissing() {
		this.assertDeserializationFailure(jsonObject -> jsonObject.remove("modificationType"), SerializationException.class);
	}

	@Test
	public void cannotDeserializeWhenModificationTypeIsInvalid() {
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("modificationType", 123), IllegalArgumentException.class);
	}

	private void assertDeserializationFailure(final Consumer<JSONObject> invalidateJsonConsumer, final Class<?> exceptionClass) {
		// Arrange:
		final MultisigMinCosignatoriesModification originalEntity =  new MultisigMinCosignatoriesModification(MultisigModificationType.MinCosignatories, 12);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(originalEntity);
		invalidateJsonConsumer.accept(jsonObject); // invalidate the json

		// Act:
		final Deserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(null));
		ExceptionAssert.assertThrows(v -> new MultisigMinCosignatoriesModification(deserializer), exceptionClass);
	}

	// endregion

	// region roundtrip

	@Test
	public void canRoundtripMultisigMinCosignatoriesModification() {
		// Arrange:
		final MultisigMinCosignatoriesModification originalEntity =  new MultisigMinCosignatoriesModification(MultisigModificationType.MinCosignatories, 12);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity, null);
		final MultisigMinCosignatoriesModification entity = new MultisigMinCosignatoriesModification(deserializer);

		// Assert:
		Assert.assertThat(entity.getModificationType(), IsEqual.equalTo(MultisigModificationType.MinCosignatories));
		Assert.assertThat(entity.getMinCosignatories(), IsEqual.equalTo(12));
	}

	// endregion
}

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
		final MultisigMinCosignatoriesModification modification = new MultisigMinCosignatoriesModification(12);

		// Assert:
		Assert.assertThat(modification.getMinCosignatories(), IsEqual.equalTo(12));
	}

	@Test
	public void cannotCreateMultisigMinCosignatoriesModificationWithZeroMinCosignatories() {
		// Assert:
		assertInvalidMinCosignatories(0);
	}

	@Test
	public void cannotCreateMultisigMinCosignatoriesModificationWithNegativeMinCosignatories() {
		// Assert:
		assertInvalidMinCosignatories(-1);
		assertInvalidMinCosignatories(-10);
	}

	private void assertInvalidMinCosignatories(final int numMinCosignatories) {
		ExceptionAssert.assertThrows(
				v -> new MultisigMinCosignatoriesModification(numMinCosignatories),
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

	private void assertDeserializationFailure(final Consumer<JSONObject> invalidateJsonConsumer, final Class<?> exceptionClass) {
		// Arrange:
		final MultisigMinCosignatoriesModification originalEntity =  new MultisigMinCosignatoriesModification(12);
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
		final MultisigMinCosignatoriesModification originalEntity =  new MultisigMinCosignatoriesModification(12);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity, null);
		final MultisigMinCosignatoriesModification entity = new MultisigMinCosignatoriesModification(deserializer);

		// Assert:
		Assert.assertThat(entity.getMinCosignatories(), IsEqual.equalTo(12));
	}

	// endregion
}

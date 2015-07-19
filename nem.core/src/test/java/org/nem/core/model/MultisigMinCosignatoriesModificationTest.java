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
	public void canCreateMultisigMinCosignatoriesModificationWithPositiveRelativeChange() {
		// Assert:
		this.assertCanCreateMultisigMinCosignatoriesModification(12);
	}

	@Test
	public void canCreateMultisigMinCosignatoriesModificationWithNoRelativeChange() {
		// Assert:
		this.assertCanCreateMultisigMinCosignatoriesModification(0);
	}

	@Test
	public void canCreateMultisigMinCosignatoriesModificationWithNegativeRelativeChange() {
		// Assert:
		this.assertCanCreateMultisigMinCosignatoriesModification(-12);
	}

	private void assertCanCreateMultisigMinCosignatoriesModification(final int relativeChange) {
		// Act:
		final MultisigMinCosignatoriesModification modification = new MultisigMinCosignatoriesModification(relativeChange);

		// Assert:
		Assert.assertThat(modification.getRelativeChange(), IsEqual.equalTo(relativeChange));
	}

	// endregion

	// region deserialization

	@Test
	public void cannotDeserializeWhenRelativeChangeIsMissing() {
		this.assertDeserializationFailure(jsonObject -> jsonObject.remove("relativeChange"), SerializationException.class);
	}

	private void assertDeserializationFailure(final Consumer<JSONObject> invalidateJsonConsumer, final Class<?> exceptionClass) {
		// Arrange:
		final MultisigMinCosignatoriesModification originalEntity = new MultisigMinCosignatoriesModification(12);
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
		final MultisigMinCosignatoriesModification originalEntity = new MultisigMinCosignatoriesModification(12);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity, null);
		final MultisigMinCosignatoriesModification entity = new MultisigMinCosignatoriesModification(deserializer);

		// Assert:
		Assert.assertThat(entity.getRelativeChange(), IsEqual.equalTo(12));
	}

	// endregion
}

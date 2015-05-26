package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.function.Consumer;

public class MultisigModificationTest {
	//region creation
	@Test
	public void canCreateMultisigModificationAdd() {
		this.assertCreateMultisigModification(MultisigModificationType.AddCosignatory);
	}

	@Test
	public void createMultisigModificationWithUnknownTypeThrows() {
		final Account account = Utils.generateRandomAccount();
		ExceptionAssert.assertThrows(v -> new MultisigModification(MultisigModificationType.Unknown, account), RuntimeException.class);
	}

	@Test
	public void createMultisigModificationWithoutCosignatoryThrows() {
		ExceptionAssert.assertThrows(v -> new MultisigModification(MultisigModificationType.AddCosignatory, null), RuntimeException.class);
	}

	private void assertCreateMultisigModification(final MultisigModificationType type) {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act
		final MultisigModification modification = new MultisigModification(type, account);

		// Assert:
		Assert.assertThat(modification.getCosignatory(), IsEqual.equalTo(account));
		Assert.assertThat(modification.getModificationType(), IsEqual.equalTo(type));
	}

	@Test
	public void deserializationFailsWhenCosignatoryIsMissing() {
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("cosignatoryAccount", null), SerializationException.class);
	}

	@Test
	public void deserializationFailsWhenModeIsInvalid() {
		// Assert:
		this.assertDeserializationFailure(jsonObject -> jsonObject.put("modificationType", 123), IllegalArgumentException.class);
	}

	private void assertDeserializationFailure(final Consumer<JSONObject> invalidateJsonConsumer, final Class<?> exceptionClass) {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(signer, cosignatory);

		final MultisigModification originalEntity = this.createMultisigModification(MultisigModificationType.AddCosignatory, cosignatory);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(originalEntity);
		invalidateJsonConsumer.accept(jsonObject); // invalidate the json

		// Act:
		final Deserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(accountLookup));
		ExceptionAssert.assertThrows(v -> new MultisigModification(deserializer), exceptionClass);
	}

	//endregion

	//region compareTo

	@Test
	public void compareToReturnsExpectedResult() {
		// Arrange:
		final List<MultisigModification> modifications = new ArrayList<>();
		modifications.add(this.createMultisigModification(MultisigModificationType.AddCosignatory, "C"));
		modifications.add(this.createMultisigModification(MultisigModificationType.AddCosignatory, "D"));
		modifications.add(this.createMultisigModification(MultisigModificationType.AddCosignatory, "E"));
		modifications.add(this.createMultisigModification(MultisigModificationType.DelCosignatory, "C"));
		modifications.add(this.createMultisigModification(MultisigModificationType.DelCosignatory, "D"));
		modifications.add(this.createMultisigModification(MultisigModificationType.DelCosignatory, "E"));

		// Assert:
		for (int i = 0; i < modifications.size(); i++) {
			for (int j = 0; j < modifications.size(); j++) {
				Assert.assertThat(modifications.get(i).compareTo(modifications.get(j)) > 0, IsEqual.equalTo(i > j));
				Assert.assertThat(modifications.get(i).compareTo(modifications.get(j)) == 0, IsEqual.equalTo(i == j));
				Assert.assertThat(modifications.get(i).compareTo(modifications.get(j)) < 0, IsEqual.equalTo(i < j));
			}
		}
	}

	private MultisigModification createMultisigModification(final MultisigModificationType modificationType, final String encodedAddress) {
		return new MultisigModification(modificationType, new Account(Address.fromEncoded(encodedAddress)));
	}

	//endregion

	// region roundtrip
	@Test
	public void canRoundtripMultisigModification() {
		// Arrange:
		final MultisigModificationType modificationType = MultisigModificationType.AddCosignatory;
		final Account cosignatory = Utils.generateRandomAccount();
		final MockAccountLookup accountLookup = MockAccountLookup.createWithAccounts(cosignatory);
		final MultisigModification originalEntity = this.createMultisigModification(modificationType, cosignatory);

		// Act:
		final MultisigModification entity = this.createRoundTrippedTransaction(originalEntity, accountLookup);

		// Assert:
		Assert.assertThat(entity.getCosignatory(), IsEqual.equalTo(cosignatory));
		Assert.assertThat(entity.getModificationType(), IsEqual.equalTo(modificationType));
	}

	private MultisigModification createRoundTrippedTransaction(
			final MultisigModification originalEntity,
			final AccountLookup accountLookup) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity, accountLookup);
		return new MultisigModification(deserializer);
	}
	// endregion

	private MultisigModification createMultisigModification(final MultisigModificationType add, final Account cosignatory) {
		return new MultisigModification(
				add,
				cosignatory);
	}
}

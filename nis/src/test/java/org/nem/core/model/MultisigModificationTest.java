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
		this.assertCreateMultisigModification(MultisigModificationType.Add);
	}

	@Test
	public void createMultisigModificationWithUnknownTypeThrows() {
		final Account account = Utils.generateRandomAccount();
		ExceptionAssert.assertThrows(v -> new MultisigModification(MultisigModificationType.Unknown, account), RuntimeException.class);
	}

	@Test
	public void createMultisigModificationWithoutCosignatoryThrows() {
		ExceptionAssert.assertThrows(v -> new MultisigModification(MultisigModificationType.Add, null), RuntimeException.class);
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

		final MultisigModification originalEntity = this.createMultisigModification(MultisigModificationType.Add, cosignatory);
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
		// TODO 20150127 J-B: can you add these to a list / array in sorted order and loop over the array to compare all pairs?
		final MultisigModification modification1 = this.createMultisigModification(MultisigModificationType.Add, "C");
		final MultisigModification modification2 = this.createMultisigModification(MultisigModificationType.Del, "C");
		final MultisigModification modification3 = this.createMultisigModification(MultisigModificationType.Add, "D");
		final MultisigModification modification4 = this.createMultisigModification(MultisigModificationType.Del, "G");
		final MultisigModification modification5 = this.createMultisigModification(MultisigModificationType.Add, "C");
		final MultisigModification modification6 = this.createMultisigModification(MultisigModificationType.Del, "C");

		// Assert:
		Assert.assertThat(modification1.compareTo(modification2) < 0, IsEqual.equalTo(true));
		Assert.assertThat(modification2.compareTo(modification1) > 0, IsEqual.equalTo(true));
		Assert.assertThat(modification1.compareTo(modification3) < 0, IsEqual.equalTo(true));
		Assert.assertThat(modification3.compareTo(modification1) > 0, IsEqual.equalTo(true));
		Assert.assertThat(modification2.compareTo(modification4) < 0, IsEqual.equalTo(true));
		Assert.assertThat(modification4.compareTo(modification2) > 0, IsEqual.equalTo(true));
		Assert.assertThat(modification1.compareTo(modification5) == 0, IsEqual.equalTo(true));
		Assert.assertThat(modification5.compareTo(modification1) == 0, IsEqual.equalTo(true));
		Assert.assertThat(modification2.compareTo(modification6) == 0, IsEqual.equalTo(true));
		Assert.assertThat(modification6.compareTo(modification2) == 0, IsEqual.equalTo(true));
	}

	// TODO 20150127 J-B: this test seems wrong
	@Test
	public void addingMultisigModificationsToListSortsList() {
		// Act:
		final List<MultisigModification> modifications = new ArrayList<>();
		modifications.add(this.createMultisigModification(MultisigModificationType.Add, "C"));
		modifications.add(this.createMultisigModification(MultisigModificationType.Del, "D"));
		modifications.add(this.createMultisigModification(MultisigModificationType.Add, "A"));
		modifications.add(this.createMultisigModification(MultisigModificationType.Del, "F"));
		modifications.add(this.createMultisigModification(MultisigModificationType.Add, "B"));
		modifications.add(this.createMultisigModification(MultisigModificationType.Del, "E"));

		// Assert:
		for (int i = 0; i < 3; i++) {
			Assert.assertThat(modifications.get(i).getModificationType(), IsEqual.equalTo(MultisigModificationType.Add));
			Assert.assertThat(
					modifications.get(i).getCosignatory().getAddress().getEncoded(),
					IsEqual.equalTo(Character.toString((char)(i + (int)'A'))));
			Assert.assertThat(modifications.get(i + 3).getModificationType(), IsEqual.equalTo(MultisigModificationType.Del));
			Assert.assertThat(
					modifications.get(i + 3).getCosignatory().getAddress().getEncoded(),
					IsEqual.equalTo(Character.toString((char)(i + (int)'D'))));
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
		final MultisigModificationType modificationType = MultisigModificationType.Add;
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

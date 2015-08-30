package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class MosaicDefinitionTest {

	// region ctor

	@Test
	public void canCreateMosaicDefinitionAroundValidParameters() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicLevy levy = Utils.createMosaicLevy();
		final MosaicProperties properties = Utils.createMosaicProperties();

		// Act:
		final MosaicDefinition mosaicDefinition = createMosaicDefinition(creator, properties, levy);

		// Assert:
		assertMosaicDefinitionProperties(mosaicDefinition, creator, properties, levy);
	}

	@Test
	public void canCreateMosaicDefinitionWithoutLevyParameter() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();

		// Act:
		final MosaicDefinition mosaicDefinition = createMosaicDefinition(creator, properties);

		// Assert:
		assertMosaicDefinitionProperties(mosaicDefinition, creator, properties, null);
	}

	@Test
	public void cannotCreateMosaicDefinitionWithNullParameters() {
		// Assert:
		Arrays.asList("creator", "id", "description", "properties")
				.forEach(MosaicDefinitionTest::assertMosaicDefinitionCannotBeCreatedWithNull);
	}

	private static void assertMosaicDefinitionCannotBeCreatedWithNull(final String parameterName) {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new MosaicDefinition(
						parameterName.equals("creator") ? null : Utils.generateRandomAccount(),
						parameterName.equals("id") ? null : new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers"),
						parameterName.equals("description") ? null : new MosaicDescriptor("precious vouchers"),
						parameterName.equals("properties") ? null : Utils.createMosaicProperties(),
						null),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains(parameterName));
	}

	// endregion

	// region isMosaicLevyPresent

	@Test
	public void isMosaicLevyPresentReturnsTrueIfLevyIsNonZero() {
		// Assert:
		assertIsMosaicLevyPresent(createMosaicLevy(Quantity.fromValue(123)), true);
	}

	@Test
	public void isMosaicLevyPresentReturnsTrueIfLevyIsZero() {
		// Assert:
		assertIsMosaicLevyPresent(createMosaicLevy(Quantity.ZERO), true);
	}

	@Test
	public void isMosaicLevyPresentReturnsFalseIfLevyIsUnspecified() {
		// Assert:
		assertIsMosaicLevyPresent(null, false);
	}

	private static void assertIsMosaicLevyPresent(final MosaicLevy levy, final boolean expected) {
		// Arrange:
		final MosaicDefinition mosaicDefinition = createMosaicDefinition(
				Utils.generateRandomAccount(),
				Utils.createMosaicProperties(),
				levy);

		// Assert:
		Assert.assertThat(mosaicDefinition.isMosaicLevyPresent(), IsEqual.equalTo(expected));
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripMosaicDefinition() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();
		final MosaicLevy levy = Utils.createMosaicLevy();
		final MosaicDefinition original = createMosaicDefinition(creator, properties, levy);

		// Act:
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		assertMosaicDefinitionProperties(mosaicDefinition, creator, properties, levy);
	}

	@Test
	public void canDeserializeMosaicDefinitionWithoutLevyParameter() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();
		final MosaicDefinition original = createMosaicDefinition(creator, properties);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(original);
		jsonObject.remove("levy");
		final JsonDeserializer deserializer = Utils.createDeserializer(jsonObject);

		// Act:
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(deserializer);

		// Assert:
		assertMosaicDefinitionProperties(mosaicDefinition, creator, properties, null);
	}

	@Test
	public void cannotDeserializeMosaicDefinitionWithMissingRequiredParameter() {
		// Assert:
		Arrays.asList("creator", "id", "description", "properties")
				.forEach(n -> assertCannotDeserializeWithoutKey(n, MissingRequiredPropertyException.class));
	}

	private static void assertCannotDeserializeWithoutKey(final String key, final Class expectedExceptionClass) {
		// Arrange:
		final MosaicDefinition mosaicDefinition = Utils.createMosaicDefinition(Utils.generateRandomAccount());
		final JSONObject jsonObject = JsonSerializer.serializeToJson(mosaicDefinition);
		jsonObject.remove(key);

		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(new MockAccountLookup()));

		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicDefinition(deserializer), expectedExceptionClass);
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnAsteriskConcatenatedNamespaceIdAndMosaicId() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = createMosaicDefinition("alice.vouchers", "Alice's vouchers");

		// Act:
		final String uniqueId = mosaicDefinition.toString();

		// Assert:
		Assert.assertThat(uniqueId, IsEqual.equalTo("alice.vouchers * Alice's vouchers"));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicDefinition mosaicDefinition = createMosaicDefinitionA("Alice's vouchers");

		// Assert:
		for (final Map.Entry<String, MosaicDefinition> entry : createMosaicDefinitionsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(mosaicDefinition)) : IsEqual.equalTo(mosaicDefinition));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(mosaicDefinition)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(mosaicDefinition)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = createMosaicDefinitionA("Alice's vouchers").hashCode();

		// Assert:
		for (final Map.Entry<String, MosaicDefinition> entry : createMosaicDefinitionsForEqualityTests().entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static Map<String, MosaicDefinition> createMosaicDefinitionsForEqualityTests() {
		return new HashMap<String, MosaicDefinition>() {
			{
				this.put("default", createMosaicDefinitionA("Alice's vouchers"));
				this.put("diff-id", createMosaicDefinitionA("Bob's vouchers"));
				this.put("diff-id-case", createMosaicDefinitionA("ALICE'S vouchers"));
				this.put("same-id-diff-everything", createMosaicDefinitionB("Alice's vouchers"));
				this.put("diff-id-diff-everything", createMosaicDefinitionB("Bob's vouchers"));
			}
		};
	}

	private static boolean isDiffExpected(final String propertyName) {
		switch (propertyName) {
			case "diff-id":
			case "diff-id-diff-everything":
				return true;
		}

		return false;
	}

	private static MosaicDefinition createMosaicDefinitionA(final String name) {
		return new MosaicDefinition(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId("xyz"), name),
				new MosaicDescriptor("precious vouchers"),
				Utils.createMosaicProperties(),
				createMosaicLevy(Quantity.fromValue(142)));
	}

	private static MosaicDefinition createMosaicDefinitionB(final String name) {
		return new MosaicDefinition(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId("xyz"), name),
				new MosaicDescriptor("silver coins"),
				new DefaultMosaicProperties(new Properties()),
				createMosaicLevy(Quantity.fromValue(123)));
	}

	// endregion

	private static void assertMosaicDefinitionProperties(
			final MosaicDefinition mosaicDefinition,
			final Account creator,
			final MosaicProperties properties,
			final MosaicLevy levy) {
		// Assert:
		Assert.assertThat(mosaicDefinition.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers")));
		Assert.assertThat(mosaicDefinition.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection(), IsEquivalent.equivalentTo(properties.asCollection()));
		Assert.assertThat(mosaicDefinition.getMosaicLevy(), null == levy ? IsNull.nullValue() : IsEqual.equalTo(levy));
	}

	private static MosaicDefinition createMosaicDefinition(final String namespaceId, final String name) {
		return new MosaicDefinition(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId(namespaceId), name),
				new MosaicDescriptor("precious vouchers"),
				Utils.createMosaicProperties(),
				null);
	}

	private static MosaicDefinition createMosaicDefinition(
			final Account creator,
			final MosaicProperties properties) {
		return new MosaicDefinition(
				creator,
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				properties,
				null);
	}

	private static MosaicDefinition createMosaicDefinition(
			final Account creator,
			final MosaicProperties properties,
			final MosaicLevy levy) {
		return new MosaicDefinition(
				creator,
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				properties,
				levy);
	}

	private static MosaicLevy createMosaicLevy(final Quantity fee) {
		return new MosaicLevy(
				MosaicTransferFeeType.Absolute,
				Utils.generateRandomAccount(),
				Utils.createMosaicId(2),
				fee);
	}
}

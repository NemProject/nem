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
		final MosaicTransferFeeInfo feeInfo = Utils.createMosaicTransferFeeInfo();
		final MosaicProperties properties = Utils.createMosaicProperties();

		// Act:
		final MosaicDefinition mosaicDefinition = createMosaicDefinition(creator, properties, feeInfo);

		// Assert:
		assertMosaicDefinitionProperties(mosaicDefinition, creator, properties, feeInfo);
	}

	@Test
	public void canCreateMosaicDefinitionWithoutFeeInfoParameter() {
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

	// region isTransferFeeAvailable

	@Test
	public void isTransferFeeAvailableReturnsTrueIfFeeIsNonZero() {
		// Assert:
		assertIsTransferFeeAvailable(createTransferFeeInfo(Quantity.fromValue(123)), true);
	}

	@Test
	public void isTransferFeeAvailableReturnsTrueIfFeeIsZero() {
		// Assert:
		assertIsTransferFeeAvailable(createTransferFeeInfo(Quantity.ZERO), true);
	}

	@Test
	public void isTransferFeeAvailableReturnsFalseIfFeeIsUnspecified() {
		// Assert:
		assertIsTransferFeeAvailable(null, false);
	}

	private static void assertIsTransferFeeAvailable(final MosaicTransferFeeInfo feeInfo, final boolean expectedIsTransferFeeAvailable) {
		// Arrange:
		final MosaicDefinition mosaicDefinition = createMosaicDefinition(
				Utils.generateRandomAccount(),
				Utils.createMosaicProperties(),
				feeInfo);

		// Assert:
		Assert.assertThat(mosaicDefinition.isTransferFeeAvailable(), IsEqual.equalTo(expectedIsTransferFeeAvailable));
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripMosaicDefinition() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();
		final MosaicTransferFeeInfo feeInfo = Utils.createMosaicTransferFeeInfo();
		final MosaicDefinition original = createMosaicDefinition(creator, properties, feeInfo);

		// Act:
		final MosaicDefinition mosaicDefinition = new MosaicDefinition(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		assertMosaicDefinitionProperties(mosaicDefinition, creator, properties, feeInfo);
	}

	@Test
	public void canDeserializeMosaicDefinitionWithoutTransferFeeInfoParameter() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();
		final MosaicDefinition original = createMosaicDefinition(creator, properties);
		final JSONObject jsonObject = JsonSerializer.serializeToJson(original);
		jsonObject.remove("transferFeeInfo");
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
				createTransferFeeInfo(Quantity.fromValue(142)));
	}

	private static MosaicDefinition createMosaicDefinitionB(final String name) {
		return new MosaicDefinition(
				Utils.generateRandomAccount(),
				new MosaicId(new NamespaceId("xyz"), name),
				new MosaicDescriptor("silver coins"),
				new DefaultMosaicProperties(new Properties()),
				createTransferFeeInfo(Quantity.fromValue(123)));
	}

	// endregion

	private static void assertMosaicDefinitionProperties(
			final MosaicDefinition mosaicDefinition,
			final Account creator,
			final MosaicProperties properties,
			final MosaicTransferFeeInfo feeInfo) {
		// Assert:
		Assert.assertThat(mosaicDefinition.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers")));
		Assert.assertThat(mosaicDefinition.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection(), IsEquivalent.equivalentTo(properties.asCollection()));
		Assert.assertThat(mosaicDefinition.getTransferFeeInfo(), null == feeInfo ? IsNull.nullValue() : IsEqual.equalTo(feeInfo));
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
			final MosaicTransferFeeInfo feeInfo) {
		return new MosaicDefinition(
				creator,
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				properties,
				feeInfo);
	}

	private static MosaicTransferFeeInfo createTransferFeeInfo(final Quantity fee) {
		return new MosaicTransferFeeInfo(
				MosaicTransferFeeType.Absolute,
				Utils.generateRandomAccount(),
				fee);
	}
}

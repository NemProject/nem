package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class MosaicTest {

	// region ctor

	@Test
	public void canCreateMosaicFromValidParameters() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();

		// Act:
		final Mosaic mosaic = new Mosaic(
				creator,
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				properties);

		// Assert:
		assertMosaicProperties(mosaic, creator, properties);
	}

	@Test
	public void cannotCreateMosaicWithNullCreator() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				null,
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullId() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				null,
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullDescriptor() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				null,
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullNamespaceId() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				null,
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullAmount() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				null,
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithNullProperties() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateMosaicWithZeroAmount() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.ZERO,
				Utils.createMosaicProperties()), IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripMosaic() {
		// Arrange:
		final Account creator = Utils.generateRandomAccount();
		final MosaicProperties properties = Utils.createMosaicProperties();
		final Mosaic original = new Mosaic(
				creator,
				new MosaicId("Alice's vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				properties);

		// Act:
		final Mosaic mosaic = new Mosaic(Utils.roundtripSerializableEntity(original, new MockAccountLookup()));

		// Assert:
		assertMosaicProperties(mosaic, creator, properties);
	}

	// TODO 20150207 J-J should we have tests that validate we can't deserialize with zero amount / null values
	// TODO 20150703 BR -> J: yea
	@Test
	public void cannotDeserializeMosaicWithMissingRequiredParameter() {
		assertCannotDeserialize("creator", null, MissingRequiredPropertyException.class);
		assertCannotDeserialize("id", null, MissingRequiredPropertyException.class);
		assertCannotDeserialize("description", null, MissingRequiredPropertyException.class);
		assertCannotDeserialize("namespaceId", null, MissingRequiredPropertyException.class);
		assertCannotDeserialize("amount", null, MissingRequiredPropertyException.class);
		assertCannotDeserialize("properties", null, MissingRequiredPropertyException.class);
		assertCannotDeserialize("children", null, MissingRequiredPropertyException.class);
	}

	@Test
	public void cannotDeserializeMosaicWithZeroAmount() {
		assertCannotDeserialize("amount", 0L, IllegalArgumentException.class);
	}

	private static void assertCannotDeserialize(final String key, final Object value, final Class expectedExceptionClass) {
		// Arrange:
		final Mosaic mosaic = Utils.createMosaic(Utils.generateRandomAccount());
		final JSONObject jsonObject = JsonSerializer.serializeToJson(mosaic);
		if (null == value) {
			jsonObject.remove(key);
		} else {
			jsonObject.put(key, value);
		}

		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(new MockAccountLookup()));

		// Assert:
		ExceptionAssert.assertThrows(v -> new Mosaic(deserializer),	expectedExceptionClass);
	}

	private void assertMosaicProperties(final Mosaic mosaic, final Account creator, final MosaicProperties properties) {
		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(creator));
		Assert.assertThat(mosaic.getId(), IsEqual.equalTo(new MosaicId("Alice's vouchers")));
		Assert.assertThat(mosaic.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaic.getNamespaceId(), IsEqual.equalTo(new NamespaceId("alice.vouchers")));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
		Assert.assertThat(mosaic.getProperties().asCollection(), IsEquivalent.equivalentTo(properties.asCollection()));
		Assert.assertThat(mosaic.getChildren().isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnAsteriskConcatenatedNamespaceIdAndMosaicId() {
		// Arrange:
		final Mosaic mosaic = createMosaic("Alice's vouchers", "alice.vouchers");

		// Act:
		final String uniqueId = mosaic.toString();

		// Assert:
		Assert.assertThat(uniqueId, IsEqual.equalTo("alice.vouchers*Alice's vouchers"));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Mosaic mosaic = createMosaic("Alice's vouchers", "alice.vouchers");
		final Map<String, Mosaic> infoMap = createMosaicsForEqualityTests();

		// Assert:
		Assert.assertThat(infoMap.get("default"), IsEqual.equalTo(mosaic));
		Assert.assertThat(infoMap.get("default2"), IsEqual.equalTo(mosaic));
		Assert.assertThat(infoMap.get("diff-id"), IsNot.not(IsEqual.equalTo(mosaic)));
		Assert.assertThat(infoMap.get("diff-namespaceId"), IsNot.not(IsEqual.equalTo(mosaic)));
		Assert.assertThat(infoMap.get("diff-both"), IsNot.not(IsEqual.equalTo(mosaic)));
		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(mosaic)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(mosaic)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = createMosaic("Alice's vouchers", "alice.vouchers").hashCode();
		final Map<String, Mosaic> infoMap = createMosaicsForEqualityTests();

		// Assert:
		Assert.assertThat(infoMap.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("default2").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(infoMap.get("diff-id").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-namespaceId").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(infoMap.get("diff-both").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	private static Map<String, Mosaic> createMosaicsForEqualityTests() {
		return new HashMap<String, Mosaic>() {
			{
				this.put("default", createMosaic("Alice's vouchers", "alice.vouchers"));
				this.put("default2", createMosaic("Alice's vouchers", "Alice.Vouchers"));
				this.put("diff-id", createMosaic("Bob's vouchers", "alice.vouchers"));
				this.put("diff-namespaceId", createMosaic("Alice's vouchers", "bob.vouchers"));
				this.put("diff-both", createMosaic("Bob's vouchers", "bob.vouchers"));
			}
		};
	}

	private static Mosaic createMosaic(final String id, final String namespaceId) {
		return new Mosaic(
				Utils.generateRandomAccount(),
				new MosaicId(id),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId(namespaceId),
				GenericAmount.fromValue(123),
				Utils.createMosaicProperties());
	}

	// endregion
}

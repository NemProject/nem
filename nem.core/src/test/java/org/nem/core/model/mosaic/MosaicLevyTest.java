package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class MosaicLevyTest {

	// region ctor

	@Test
	public void canCreateMosaicLevy() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();

		// Act:
		final MosaicLevy levy = createMosaicLevy(recipient);

		// Assert:
		assertMosaicLevy(levy, recipient);
	}

	@Test
	public void cannotCreateMosaicLevyWithInvalidRecipient() {
		// Arrange:
		final Account recipient = new Account(Address.fromEncoded("FOO"));

		// Act:
		ExceptionAssert.assertThrows(
				v -> createMosaicLevy(recipient),
				IllegalArgumentException.class);
	}

	// endregion

	// serialization

	@Test
	public void canDeserializeWithValidRecipient() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final Deserializer deserializer = createDeserializer(recipient.getAddress().getEncoded());

		// Assert:
		final MosaicLevy levy = new MosaicLevy(deserializer);

		// Assert:
		assertMosaicLevy(levy, recipient);
	}

	@Test
	public void cannotDeserializeWithInvalidRecipient() {
		// Arrange:
		final Deserializer deserializer = createDeserializer("foo");

		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicLevy(deserializer), IllegalArgumentException.class);
	}

	@Test
	public void canRoundTripMosaicLevy() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final MosaicLevy original = createMosaicLevy(recipient);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, new MockAccountLookup());

		// Act:
		final MosaicLevy levy = new MosaicLevy(deserializer);

		// Assert:
		assertMosaicLevy(levy, recipient);
	}

	// endregion

	//region equals / hashCode

	private static Map<String, MosaicLevy> createLeviesForEqualityTests(final Account recipient) {
		final MosaicId mosaicId = Utils.createMosaicId(5);
		final Quantity quantity = Quantity.fromValue(123);
		return new HashMap<String, MosaicLevy>() {
			{
				this.put("default", createMosaicLevy(recipient));
				this.put("diff-feeType", createMosaicLevy(MosaicTransferFeeType.Percentile, recipient, mosaicId, quantity));
				this.put("diff-recipient", createMosaicLevy(MosaicTransferFeeType.Absolute, Utils.generateRandomAccount(), mosaicId, quantity));
				this.put("diff-mosaicId", createMosaicLevy(MosaicTransferFeeType.Absolute, recipient, Utils.createMosaicId(12), quantity));
				this.put("diff-quantity", createMosaicLevy(MosaicTransferFeeType.Absolute, recipient, mosaicId, Quantity.fromValue(321)));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final MosaicLevy levy = createMosaicLevy(recipient);

		// Assert:
		for (final Map.Entry<String, MosaicLevy> entry : createLeviesForEqualityTests(recipient).entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(levy)) : IsEqual.equalTo(levy));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(levy)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(levy)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Account recipient = Utils.generateRandomAccount();
		final int hashCode = createMosaicLevy(recipient).hashCode();

		// Assert:
		for (final Map.Entry<String, MosaicLevy> entry : createLeviesForEqualityTests(recipient).entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static boolean isDiffExpected(final String propertyName) {
		return !propertyName.equals("default");
	}

	// endregion

	private static void assertMosaicLevy(final MosaicLevy levy, final Account recipient) {
		// Assert:
		Assert.assertThat(levy.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(levy.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(levy.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(5)));
		Assert.assertThat(levy.getFee(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	private static MosaicLevy createMosaicLevy(final Account recipient) {
		return createMosaicLevy(MosaicTransferFeeType.Absolute, recipient, Utils.createMosaicId(5), Quantity.fromValue(123));
	}

	private static MosaicLevy createMosaicLevy(
			final MosaicTransferFeeType feeType,
			final Account recipient,
			final MosaicId mosaicId,
			final Quantity quantity) {
		return new MosaicLevy(feeType, recipient, mosaicId, quantity);
	}

	private static Deserializer createDeserializer(final String recipient) {
		final MosaicLevy levy = createMosaicLevy(Utils.generateRandomAccount());
		final JSONObject jsonObject = JsonSerializer.serializeToJson(levy);
		jsonObject.put("recipient", recipient);
		return Utils.createDeserializer(jsonObject);
	}
}

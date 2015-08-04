package org.nem.core.model.mosaic;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.*;

public class MosaicTransferFeeInfoTest {

	// region ctor

	@Test
	public void canCreateMosaicTransferFeeInfo() {
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();

		// Act:
		final MosaicTransferFeeInfo info = createMosaicTransferFeeInfo(recipient);

		// Assert:
		assertMosaicTransferFeeInfo(info, recipient);
	}

	// endregion

	// region ctor

	@Test
	public void defaultFeeInfoReturnsExpectedMosaicTransferFeeInfo() {
		// Act:
		final MosaicTransferFeeInfo info = MosaicTransferFeeInfo.defaultFeeInfo();

		// Assert:
		Assert.assertThat(info.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(info.getRecipient(), IsEqual.equalTo(MosaicConstants.MOSAIC_ADMITTER.getAddress()));
		Assert.assertThat(info.getMosaicId(), IsEqual.equalTo(MosaicConstants.MOSAIC_DEFINITION_XEM.getId()));
		Assert.assertThat(info.getFee(), IsEqual.equalTo(Quantity.ZERO));
	}

	// endregion

	// serialization

	@Test
	public void cannotDeserializeWithInvalidRecipient() {
		// Arrange:
		final Deserializer deserializer = createDeserializer("foo");

		// Assert:
		ExceptionAssert.assertThrows(v -> new MosaicTransferFeeInfo(deserializer), IllegalArgumentException.class);
	}

	@Test
	public void canDeserializeWithValidRecipient() {
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();
		final Deserializer deserializer = createDeserializer(recipient.getEncoded());

		// Assert:
		final MosaicTransferFeeInfo info =  new MosaicTransferFeeInfo(deserializer);

		// Assert:
		assertMosaicTransferFeeInfo(info, recipient);
	}

	@Test
	public void canRoundTripMosaicTransferFeeInfo() {
		// Arrange:
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();
		final MosaicTransferFeeInfo original = createMosaicTransferFeeInfo(recipient);
		final Deserializer deserializer = Utils.roundtripSerializableEntity(original, null);

		// Act:
		final MosaicTransferFeeInfo info = new MosaicTransferFeeInfo(deserializer);

		// Assert:
		assertMosaicTransferFeeInfo(info, recipient);
	}

	// endregion

	//region equals / hashCode

	private static Map<String, MosaicTransferFeeInfo> createFeeInfosForEqualityTests(final Address recipient) {
		final MosaicId mosaicId = Utils.createMosaicId(5);
		final Quantity quantity = Quantity.fromValue(123);
		return new HashMap<String, MosaicTransferFeeInfo>() {
			{
				this.put("default", createMosaicTransferFeeInfo(recipient));
				this.put("diff-feeType", createMosaicTransferFeeInfo(MosaicTransferFeeType.Percentile, recipient, mosaicId, quantity));
				this.put("diff-recipient", createMosaicTransferFeeInfo(MosaicTransferFeeType.Absolute, Utils.generateRandomAddress(), mosaicId, quantity));
				this.put("diff-mosaicId", createMosaicTransferFeeInfo(MosaicTransferFeeType.Absolute, recipient, Utils.createMosaicId(2), quantity));
				this.put("diff-quantity", createMosaicTransferFeeInfo(MosaicTransferFeeType.Absolute, recipient, mosaicId, Quantity.fromValue(321)));
			}
		};
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();
		final MosaicTransferFeeInfo feeInfo = createMosaicTransferFeeInfo(recipient);

		// Assert:
		for (final Map.Entry<String, MosaicTransferFeeInfo> entry : createFeeInfosForEqualityTests(recipient).entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(feeInfo)) : IsEqual.equalTo(feeInfo));
		}

		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(feeInfo)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(feeInfo)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Address recipient = Utils.generateRandomAddress();
		final int hashCode = createMosaicTransferFeeInfo(recipient).hashCode();

		// Assert:
		for (final Map.Entry<String, MosaicTransferFeeInfo> entry : createFeeInfosForEqualityTests(recipient).entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static boolean isDiffExpected(final String propertyName) {
		return !propertyName.equals("default");
	}

	// endregion

	private static void assertMosaicTransferFeeInfo(final MosaicTransferFeeInfo info, final Address recipient) {
		// Assert:
		Assert.assertThat(info.getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(info.getRecipient(), IsEqual.equalTo(recipient));
		Assert.assertThat(info.getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(5)));
		Assert.assertThat(info.getFee(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	private static MosaicTransferFeeInfo createMosaicTransferFeeInfo(final Address recipient) {
		return createMosaicTransferFeeInfo(MosaicTransferFeeType.Absolute, recipient, Utils.createMosaicId(5), Quantity.fromValue(123));
	}
	private static MosaicTransferFeeInfo createMosaicTransferFeeInfo(
			final MosaicTransferFeeType feeType,
			final Address recipient,
			final MosaicId mosaicId,
			final Quantity quantity) {
		return new MosaicTransferFeeInfo(feeType, recipient, mosaicId, quantity);
	}

	private static Deserializer createDeserializer(final String recipient) {
		final JsonSerializer serializer = new JsonSerializer();
		final MosaicTransferFeeInfo info = createMosaicTransferFeeInfo(Utils.generateRandomAddress());
		info.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();
		jsonObject.put("recipient", recipient);
		return new JsonDeserializer(serializer.getObject(), null);
	}
}

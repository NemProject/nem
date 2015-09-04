package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class HashTest {

	private static final byte[] TEST_BYTES = new byte[] { 0x22, (byte)0xAB, 0x71 };
	private static final byte[] MODIFIED_TEST_BYTES = new byte[] { 0x22, (byte)0xAB, 0x72 };

	//region constants

	@Test
	public void zeroHashIsInitializedCorrectly() {
		// Assert:
		Assert.assertThat(Hash.ZERO, IsEqual.equalTo(new Hash(new byte[32])));
	}

	//endregion

	//region constructors / factories

	@Test
	public void canCreateFromBytes() {
		// Arrange:
		final Hash hash = new Hash(TEST_BYTES);

		// Assert:
		Assert.assertThat(hash.getRaw(), IsEqual.equalTo(TEST_BYTES));
	}

	@Test
	public void canCreateFromHexString() {
		// Arrange:
		final Hash hash = Hash.fromHexString("227F");

		// Assert:
		Assert.assertThat(hash.getRaw(), IsEqual.equalTo(new byte[] { 0x22, 0x7F }));
	}

	@Test(expected = CryptoException.class)
	public void cannotCreateAroundMalformedHexString() {
		// Act:
		Hash.fromHexString("22G75");
	}

	//endregion

	//region serializer

	@Test
	public void hashCanBeRoundTripped() {
		// Act:
		final Hash hash = createRoundTrippedHash(new Hash(TEST_BYTES));

		// Assert:
		Assert.assertThat(hash, IsEqual.equalTo(new Hash(TEST_BYTES)));
	}

	public static Hash createRoundTrippedHash(final Hash originalKey) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalKey, null);
		return new Hash(deserializer);
	}

	//endregion

	//region equals / hash

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Hash hash = new Hash(TEST_BYTES);

		// Assert:
		Assert.assertThat(new Hash(TEST_BYTES), IsEqual.equalTo(hash));
		Assert.assertThat(new Hash(MODIFIED_TEST_BYTES), IsNot.not(IsEqual.equalTo(hash)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(hash)));
		Assert.assertThat(TEST_BYTES, IsNot.not(IsEqual.equalTo((Object)hash)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Hash hash = new Hash(TEST_BYTES);
		final int hashCode = hash.hashCode();

		// Assert:
		Assert.assertThat(new Hash(TEST_BYTES).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new Hash(MODIFIED_TEST_BYTES).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region getShortId

	@Test
	public void shortIdIsCalculatedCorrectly() {
		// Arrange:
		final Hash hash = new Hash(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 });

		// Assert:
		Assert.assertThat(hash.getShortId(), IsEqual.equalTo(0x0001020304050607L));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsHexRepresentation() {
		// Assert:
		Assert.assertThat(new Hash(TEST_BYTES).toString(), IsEqual.equalTo("22ab71"));
	}

	//endregion
}
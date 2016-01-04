package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class PublicKeyTest {

	private static final byte[] TEST_BYTES = new byte[] { 0x22, (byte)0xAB, 0x71 };
	private static final byte[] MODIFIED_TEST_BYTES = new byte[] { 0x22, (byte)0xAB, 0x72 };

	//region constructors / factories

	@Test
	public void canCreateFromBytes() {
		// Arrange:
		final PublicKey key = new PublicKey(TEST_BYTES);

		// Assert:
		Assert.assertThat(key.getRaw(), IsEqual.equalTo(TEST_BYTES));
	}

	@Test
	public void canCreateFromHexString() {
		// Arrange:
		final PublicKey key = PublicKey.fromHexString("227F");

		// Assert:
		Assert.assertThat(key.getRaw(), IsEqual.equalTo(new byte[] { 0x22, 0x7F }));
	}

	@Test(expected = CryptoException.class)
	public void cannotCreateAroundMalformedHexString() {
		// Act:
		PublicKey.fromHexString("22G75");
	}

	//endregion

	//region serializer

	@Test
	public void keyCanBeRoundTripped() {
		// Act:
		final PublicKey key = createRoundTrippedKey(new PublicKey(TEST_BYTES));

		// Assert:
		Assert.assertThat(key, IsEqual.equalTo(new PublicKey(TEST_BYTES)));
	}

	private static PublicKey createRoundTrippedKey(final PublicKey originalKey) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalKey, null);
		return new PublicKey(deserializer);
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final PublicKey key = new PublicKey(TEST_BYTES);

		// Assert:
		Assert.assertThat(new PublicKey(TEST_BYTES), IsEqual.equalTo(key));
		Assert.assertThat(new PublicKey(MODIFIED_TEST_BYTES), IsNot.not(IsEqual.equalTo(key)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(key)));
		Assert.assertThat(TEST_BYTES, IsNot.not(IsEqual.equalTo((Object)key)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final PublicKey key = new PublicKey(TEST_BYTES);
		final int hashCode = key.hashCode();

		// Assert:
		Assert.assertThat(new PublicKey(TEST_BYTES).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new PublicKey(MODIFIED_TEST_BYTES).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsHexRepresentation() {
		// Assert:
		Assert.assertThat(new PublicKey(TEST_BYTES).toString(), IsEqual.equalTo("22ab71"));
	}

	//endregion
}

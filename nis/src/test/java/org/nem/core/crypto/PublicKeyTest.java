package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class PublicKeyTest {

	private final byte[] TEST_BYTES = new byte[] { 0x22, (byte)0xAB, 0x71 };
	private final byte[] MODIFIED_TEST_BYTES = new byte[] { 0x22, (byte)0xAB, 0x72 };

	//region constructors / factories

	@Test
	public void canCreateFromBytes() {
		// Arrange:
		final PublicKey key = new PublicKey(this.TEST_BYTES);

		// Assert:
		Assert.assertThat(key.getRaw(), IsEqual.equalTo(this.TEST_BYTES));
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
		final PublicKey key = createRoundTrippedKey(new PublicKey(this.TEST_BYTES));

		// Assert:
		Assert.assertThat(key, IsEqual.equalTo(new PublicKey(this.TEST_BYTES)));
	}

	public static PublicKey createRoundTrippedKey(final PublicKey originalKey) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalKey, null);
		return new PublicKey(deserializer);
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final PublicKey key = new PublicKey(this.TEST_BYTES);

		// Assert:
		Assert.assertThat(new PublicKey(this.TEST_BYTES), IsEqual.equalTo(key));
		Assert.assertThat(new PublicKey(this.MODIFIED_TEST_BYTES), IsNot.not(IsEqual.equalTo(key)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(key)));
		Assert.assertThat(this.TEST_BYTES, IsNot.not(IsEqual.equalTo((Object)key)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final PublicKey key = new PublicKey(this.TEST_BYTES);
		final int hashCode = key.hashCode();

		// Assert:
		Assert.assertThat(new PublicKey(this.TEST_BYTES).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new PublicKey(this.MODIFIED_TEST_BYTES).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsHexRepresentation() {
		// Assert:
		Assert.assertThat(new PublicKey(this.TEST_BYTES).toString(), IsEqual.equalTo("22ab71"));
	}

	//endregion

	//region projective coordinates

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfXHasWrongLength() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[9], new int[10], new int[10], new int[10]);
	}

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfXIsNull() {
		// Assert:
		new PublicKey(this.TEST_BYTES, null, new int[10], new int[10], new int[10]);
	}

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfYHasWrongLength() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[10], new int[9], new int[10], new int[10]);
	}

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfYIsNull() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[10], null, new int[10], new int[10]);
	}

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfZHasWrongLength() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[10], new int[10], new int[9], new int[10]);
	}

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfZIsNull() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[10], new int[10], null, new int[10]);
	}

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfTHasWrongLength() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[10], new int[10], new int[10], new int[9]);
	}

	@Test (expected = RuntimeException.class)
	public void ctorWithProjectiveCoordinatesThrowsIfTIsNull() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[10], new int[10], new int[10], null);
	}

	@Test
	public void canCreatePublicKeyWithProjectiveCoordinatesIfAllParamsAreCorrect() {
		// Assert:
		new PublicKey(this.TEST_BYTES, new int[10], new int[10], new int[10], new int[10]);
	}

	@Test (expected = RuntimeException.class)
	public void getXThrowsIfXIsNotSet() {
		// Arrange:
		final PublicKey pulicKey = new PublicKey(this.TEST_BYTES);

		// Assert:
		pulicKey.getX();
	}

	@Test (expected = RuntimeException.class)
	public void getYThrowsIfYIsNotSet() {
		// Arrange:
		final PublicKey pulicKey = new PublicKey(this.TEST_BYTES);

		// Assert:
		pulicKey.getY();
	}

	@Test (expected = RuntimeException.class)
	public void getTThrowsIfZIsNotSet() {
		// Arrange:
		final PublicKey pulicKey = new PublicKey(this.TEST_BYTES);

		// Assert:
		pulicKey.getZ();
	}

	@Test (expected = RuntimeException.class)
	public void getTThrowsIfTIsNotSet() {
		// Arrange:
		final PublicKey pulicKey = new PublicKey(this.TEST_BYTES);

		// Assert:
		pulicKey.getT();
	}

	//endregion

	//region delegation

	@Test
	public void isCompressedDelegatesToKeyAnalyzer() {
		final CryptoEngines.CryptoEngine engine = Mockito.mock(CryptoEngines.CryptoEngine.class);
		CryptoEngines.setDefaultEngine(engine);
		final KeyAnalyzer analyzer = Mockito.mock(KeyAnalyzer.class);
		Mockito.when(engine.createKeyAnalyzer()).thenReturn(analyzer);
		final PublicKey key = PublicKey.fromHexString("227F");

		// Act:
		key.isCompressed();

		// Assert:
		Mockito.verify(analyzer, Mockito.times(1)).isKeyCompressed(key);
	}

	//endregion
}

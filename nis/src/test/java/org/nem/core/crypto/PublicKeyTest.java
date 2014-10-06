package org.nem.core.crypto;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.ed25519.Ed25519Engine;
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

	//region delegation

	@Test
	public void isCompressedDelegatesToKeyAnalyzer() {
		final Ed25519Engine engine = Mockito.mock(Ed25519Engine.class);
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

	//region isCompressed

	@Test
	public void compressedKeyMustHaveCorrectLength() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final KeyAnalyzer analyzer = CryptoEngines.getDefaultEngine().createKeyAnalyzer();

		// Assert:
		Assert.assertThat(analyzer.isKeyCompressed(this.createKeyWithLengthDelta(publicKey, -1)), IsEqual.equalTo(false));
		Assert.assertThat(analyzer.isKeyCompressed(this.createKeyWithLengthDelta(publicKey, 0)), IsEqual.equalTo(true));
		Assert.assertThat(analyzer.isKeyCompressed(this.createKeyWithLengthDelta(publicKey, 1)), IsEqual.equalTo(false));
	}

	private PublicKey createKeyWithLengthDelta(final PublicKey key, final int lengthDelta) {
		// Arrange:
		final byte[] modifiedPublicKey = new byte[key.getRaw().length + lengthDelta];
		final int numBytesToCopy = Math.min(modifiedPublicKey.length, key.getRaw().length);
		System.arraycopy(key.getRaw(), 0, modifiedPublicKey, 0, numBytesToCopy);
		return new PublicKey(modifiedPublicKey);
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
}

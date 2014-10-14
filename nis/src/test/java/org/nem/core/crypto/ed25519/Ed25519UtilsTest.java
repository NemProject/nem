package org.nem.core.crypto.ed25519;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class Ed25519UtilsTest {

	//region prepareForScalarMultiply

	@Test
	public void prepareForScalarMultiplyReturnsClampedValue() {
		// Arrange:
		final PrivateKey privateKey = new PrivateKey(new BigInteger(Utils.generateRandomBytes(32)));

		// Act:
		final byte[] a = Ed25519Utils.prepareForScalarMultiply(privateKey).getRaw();

		// Assert:
		Assert.assertThat(a[31] & 0x40, IsEqual.equalTo(0x40));
		Assert.assertThat(a[31] & 0x80, IsEqual.equalTo(0x0));
		Assert.assertThat(a[0] & 0x7, IsEqual.equalTo(0x0));
	}

	//endregion
}

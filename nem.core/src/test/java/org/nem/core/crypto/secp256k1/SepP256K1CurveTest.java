package org.nem.core.crypto.secp256k1;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.CryptoEngines;

import java.math.BigInteger;

public class SepP256K1CurveTest {

	private static final BigInteger GROUP_ORDER = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

	@Test
	public void getNameReturnsCorrectName() {
		// Assert:
		Assert.assertThat(CryptoEngines.secp256k1Engine().getCurve().getName(), IsEqual.equalTo("secp256k1"));
	}

	@Test
	public void getNameReturnsCorrectGroupOrder() {
		// Assert:
		Assert.assertThat(CryptoEngines.secp256k1Engine().getCurve().getGroupOrder(), IsEqual.equalTo(GROUP_ORDER));
	}

	@Test
	public void getNameReturnsCorrectHalfGroupOrder() {
		// Arrange:
		final BigInteger halfGroupOrder = GROUP_ORDER.shiftRight(1);

		// Assert:
		Assert.assertThat(CryptoEngines.secp256k1Engine().getCurve().getHalfGroupOrder(), IsEqual.equalTo(halfGroupOrder));
	}
}

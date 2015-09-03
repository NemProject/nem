package org.nem.core.crypto.ed25519;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.CryptoEngines;

import java.math.BigInteger;

public class Ed25519CurveTest {

	private static final BigInteger GROUP_ORDER = BigInteger.ONE.shiftLeft(252).add(new BigInteger("27742317777372353535851937790883648493"));

	@Test
	public void getNameReturnsCorrectName() {
		// Assert:
		Assert.assertThat(CryptoEngines.ed25519Engine().getCurve().getName(), IsEqual.equalTo("ed25519"));
	}

	@Test
	public void getNameReturnsCorrectGroupOrder() {
		// Assert:
		Assert.assertThat(CryptoEngines.ed25519Engine().getCurve().getGroupOrder(), IsEqual.equalTo(GROUP_ORDER));
	}

	@Test
	public void getNameReturnsCorrectHalfGroupOrder() {
		// Arrange:
		final BigInteger halfGroupOrder = GROUP_ORDER.shiftRight(1);

		// Assert:
		Assert.assertThat(CryptoEngines.ed25519Engine().getCurve().getHalfGroupOrder(), IsEqual.equalTo(halfGroupOrder));
	}
}

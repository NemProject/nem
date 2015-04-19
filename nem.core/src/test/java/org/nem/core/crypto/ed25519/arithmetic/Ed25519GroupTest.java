package org.nem.core.crypto.ed25519.arithmetic;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.math.BigInteger;

public class Ed25519GroupTest {

	@Test
	public void groupOrderIsAsExpected() {
		// Arrange:
		final BigInteger groupOrder = new BigInteger("7237005577332262213973186563042994240857116359379907606001950938285454250989");

		// Assert:
		Assert.assertThat(groupOrder, IsEqual.equalTo(Ed25519Group.GROUP_ORDER));
	}

	@Test
	public void basePointIsAsExpected() {
		// Arrange:
		final BigInteger y = new BigInteger("4").multiply(new BigInteger("5").modInverse(Ed25519Field.P));
		final BigInteger x = MathUtils.getAffineXFromAffineY(y, false);
		final Ed25519GroupElement basePoint = Ed25519GroupElement.p2(MathUtils.toFieldElement(x), MathUtils.toFieldElement(y), Ed25519Field.ONE);

		// Assert:
		Assert.assertThat(basePoint, IsEqual.equalTo(Ed25519Group.BASE_POINT));
	}

	@Test
	public void zeroP2IsAsExpected() {
		// Arrange:
		final Ed25519GroupElement zeroP2 = Ed25519GroupElement.p2(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE);

		// Assert:
		Assert.assertThat(zeroP2, IsEqual.equalTo(Ed25519Group.ZERO_P2));
	}

	@Test
	public void zeroP3IsAsExpected() {
		// Arrange:
		final Ed25519GroupElement zeroP3 = Ed25519GroupElement.p3(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);

		// Assert:
		Assert.assertThat(zeroP3, IsEqual.equalTo(Ed25519Group.ZERO_P3));
	}

	@Test
	public void zeroPrecomputedIsAsExpected() {
		// Arrange:
		final Ed25519GroupElement zeroPrecomputed = Ed25519GroupElement.precomputed(Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);

		// Assert:
		Assert.assertThat(zeroPrecomputed, IsEqual.equalTo(Ed25519Group.ZERO_PRECOMPUTED));
	}
}

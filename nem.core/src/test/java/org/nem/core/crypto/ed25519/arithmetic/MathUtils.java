package org.nem.core.crypto.ed25519.arithmetic;

import org.nem.core.crypto.*;
import org.nem.core.utils.ArrayUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Utility class to help with calculations.
 */
public class MathUtils {
	private static final int[] EXPONENTS = {
			0,
			26,
			26 + 25,
			2 * 26 + 25,
			2 * 26 + 2 * 25,
			3 * 26 + 2 * 25,
			3 * 26 + 3 * 25,
			4 * 26 + 3 * 25,
			4 * 26 + 4 * 25,
			5 * 26 + 4 * 25
	};
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final BigInteger D = new BigInteger("-121665").multiply(new BigInteger("121666").modInverse(Ed25519Field.P));

	// region field element

	/**
	 * Converts a 2^25.5 bit representation to a BigInteger.
	 * Value: 2^EXPONENTS[0] * t[0] + 2^EXPONENTS[1] * t[1] + ... + 2^EXPONENTS[9] * t[9]
	 *
	 * @param t The 2^25.5 bit representation.
	 * @return The BigInteger.
	 */
	public static BigInteger toBigInteger(final int[] t) {
		BigInteger b = BigInteger.ZERO;
		for (int i = 0; i < 10; i++) {
			b = b.add(BigInteger.ONE.multiply(BigInteger.valueOf(t[i])).shiftLeft(EXPONENTS[i]));
		}

		return b;
	}

	/**
	 * Converts a 2^8 bit representation to a BigInteger.
	 * Value: bytes[0] + 2^8 * bytes[1] + ...
	 *
	 * @param bytes The 2^8 bit representation.
	 * @return The BigInteger.
	 */
	public static BigInteger toBigInteger(final byte[] bytes) {
		BigInteger b = BigInteger.ZERO;
		for (int i = 0; i < bytes.length; i++) {
			b = b.add(BigInteger.ONE.multiply(BigInteger.valueOf(bytes[i] & 0xff)).shiftLeft(i * 8));
		}

		return b;
	}

	/**
	 * Converts an encoded field element to a BigInteger.
	 *
	 * @param encoded The encoded field element.
	 * @return The BigInteger.
	 */
	public static BigInteger toBigInteger(final Ed25519EncodedFieldElement encoded) {
		return toBigInteger(encoded.getRaw());
	}

	/**
	 * Converts a field element to a BigInteger.
	 *
	 * @param f The field element.
	 * @return The BigInteger.
	 */
	public static BigInteger toBigInteger(final Ed25519FieldElement f) {
		return toBigInteger(f.encode().getRaw());
	}

	/**
	 * Converts a BigInteger to a field element.
	 *
	 * @param b The BigInteger.
	 * @return The field element.
	 */
	public static Ed25519FieldElement toFieldElement(final BigInteger b) {
		return new Ed25519EncodedFieldElement(toByteArray(b)).decode();
	}

	/**
	 * Converts a BigInteger to a little endian 32 byte representation.
	 *
	 * @param b The BigInteger.
	 * @return The 32 byte representation.
	 */
	public static byte[] toByteArray(final BigInteger b) {
		if (b.compareTo(BigInteger.ONE.shiftLeft(256)) >= 0) {
			throw new RuntimeException("only numbers < 2^256 are allowed");
		}
		final byte[] bytes = new byte[32];
		final byte[] original = b.toByteArray();

		// Although b < 2^256, original can have length > 32 with some bytes set to 0.
		final int offset = original.length > 32 ? original.length - 32 : 0;
		for (int i = 0; i < original.length - offset; i++) {
			bytes[original.length - i - offset - 1] = original[i + offset];
		}

		return bytes;
	}

	/**
	 * Converts a BigInteger to an encoded field element.
	 *
	 * @param b The BigInteger.
	 * @return The encoded field element.
	 */
	public static Ed25519EncodedFieldElement toEncodedFieldElement(final BigInteger b) {
		return new Ed25519EncodedFieldElement(toByteArray(b));
	}

	/**
	 * Reduces an encoded field element modulo the group order and returns the result.
	 *
	 * @param encoded The encoded field element.
	 * @return The mod group order reduced encoded field element.
	 */
	public static Ed25519EncodedFieldElement reduceModGroupOrder(final Ed25519EncodedFieldElement encoded) {
		final BigInteger b = toBigInteger(encoded).mod(Ed25519Group.GROUP_ORDER);
		return toEncodedFieldElement(b);
	}

	/**
	 * Calculates (a * b + c) mod group order and returns the result.
	 * a, b and c are given in 2^8 bit representation.
	 *
	 * @param a The first integer.
	 * @param b The second integer.
	 * @param c The third integer.
	 * @return The mod group order reduced result.
	 */
	public static Ed25519EncodedFieldElement multiplyAndAddModGroupOrder(
			final Ed25519EncodedFieldElement a,
			final Ed25519EncodedFieldElement b,
			final Ed25519EncodedFieldElement c) {
		final BigInteger result = toBigInteger(a).multiply(toBigInteger(b)).add(toBigInteger(c)).mod(Ed25519Group.GROUP_ORDER);
		return toEncodedFieldElement(result);
	}

	/**
	 * Creates and returns a random byte array of given length.
	 *
	 * @param length The desired length.
	 * @return The random byte array.
	 */
	public static byte[] getRandomByteArray(final int length) {
		final byte[] bytes = new byte[length];
		RANDOM.nextBytes(bytes);
		return bytes;
	}

	/**
	 * Gets a random field element where |t[i]| <= 2^24 for 0 <= i <= 9.
	 *
	 * @return The field element.
	 */
	public static Ed25519FieldElement getRandomFieldElement() {
		final int[] t = new int[10];
		for (int j = 0; j < 10; j++) {
			t[j] = RANDOM.nextInt(1 << 25) - (1 << 24);
		}
		return new Ed25519FieldElement(t);
	}

	/**
	 * Returns a random 32 byte encoded field element.
	 *
	 * @return The encoded field element.
	 */
	public static Ed25519EncodedFieldElement getRandomEncodedFieldElement(final int length) {
		final byte[] bytes = getRandomByteArray(length);
		bytes[31] &= 0x7f;
		return new Ed25519EncodedFieldElement(bytes);
	}

	// endregion

	// region group element

	/**
	 * Gets a random group element in P3 coordinates.
	 * It's NOT guaranteed that the created group element is a multiple of the base point.
	 *
	 * @return The group element.
	 */
	public static Ed25519GroupElement getRandomGroupElement() {
		final byte[] bytes = new byte[32];
		while (true) {
			try {
				RANDOM.nextBytes(bytes);
				return new Ed25519EncodedGroupElement(bytes).decode();
			} catch (final IllegalArgumentException e) {
				// Will fail in about 50%, so try again.
			}
		}
	}

	/**
	 * Gets a random encoded group element.
	 * It's NOT guaranteed that the created encoded group element is a multiple of the base point.
	 *
	 * @return The encoded group element.
	 */
	public static Ed25519EncodedGroupElement getRandomEncodedGroupElement() {
		return getRandomGroupElement().encode();
	}

	/**
	 * Creates a group element from a byte array.
	 * Bit 0 to 254 are the affine y-coordinate, bit 255 is the sign of the affine x-coordinate.
	 *
	 * @param bytes the byte array.
	 * @return The group element.
	 */
	public static Ed25519GroupElement toGroupElement(final byte[] bytes) {
		final boolean shouldBeNegative = (bytes[31] >> 7) != 0;
		bytes[31] &= 0x7f;
		final BigInteger y = MathUtils.toBigInteger(bytes);
		final BigInteger x = getAffineXFromAffineY(y, shouldBeNegative);

		return Ed25519GroupElement.p3(
				toFieldElement(x),
				toFieldElement(y),
				Ed25519Field.ONE,
				toFieldElement(x.multiply(y).mod(Ed25519Field.P)));
	}

	/**
	 * Gets the affine x-coordinate from a given affine y-coordinate and the sign of x.
	 *
	 * @param y The affine y-coordinate
	 * @param shouldBeNegative true if the negative solution should be chosen, false otherwise.
	 * @return The affine x-ccordinate.
	 */
	public static BigInteger getAffineXFromAffineY(final BigInteger y, final boolean shouldBeNegative) {
		// x = sign(x) * sqrt((y^2 - 1) / (d * y^2 + 1))
		final BigInteger u = y.multiply(y).subtract(BigInteger.ONE).mod(Ed25519Field.P);
		final BigInteger v = D.multiply(y).multiply(y).add(BigInteger.ONE).mod(Ed25519Field.P);
		BigInteger x = getSqrtOfFraction(u, v);
		if (!v.multiply(x).multiply(x).subtract(u).mod(Ed25519Field.P).equals(BigInteger.ZERO)) {
			if (!v.multiply(x).multiply(x).add(u).mod(Ed25519Field.P).equals(BigInteger.ZERO)) {
				throw new IllegalArgumentException("not a valid Ed25519GroupElement");
			}
			x = x.multiply(toBigInteger(Ed25519Field.I)).mod(Ed25519Field.P);
		}
		final boolean isNegative = x.mod(new BigInteger("2")).equals(BigInteger.ONE);
		if ((shouldBeNegative && !isNegative) || (!shouldBeNegative && isNegative)) {
			x = x.negate().mod(Ed25519Field.P);
		}

		return x;
	}

	/**
	 * Calculates and returns the square root of a fraction of u and v.
	 * The sign is unpredictable.
	 *
	 * @param u The nominator.
	 * @param v The denominator.
	 * @return Plus or minus the square root
	 */
	public static BigInteger getSqrtOfFraction(final BigInteger u, final BigInteger v) {
		final BigInteger tmp = u.multiply(v.pow(7)).modPow(BigInteger.ONE.shiftLeft(252).subtract(new BigInteger("3")), Ed25519Field.P).mod(Ed25519Field.P);
		return tmp.multiply(u).multiply(v.pow(3)).mod(Ed25519Field.P);
	}

	/**
	 * Converts a group element from one coordinate system to another.
	 * This method is a helper used to test various methods in Ed25519GroupElement.
	 *
	 * @param g The group element.
	 * @param newCoordinateSystem The desired coordinate system.
	 * @return The same group element in the new coordinate system.
	 */
	public static Ed25519GroupElement toRepresentation(final Ed25519GroupElement g, final CoordinateSystem newCoordinateSystem) {
		final BigInteger x;
		final BigInteger y;
		final BigInteger gX = toBigInteger(g.getX().encode());
		final BigInteger gY = toBigInteger(g.getY().encode());
		final BigInteger gZ = toBigInteger(g.getZ().encode());
		final BigInteger gT = null == g.getT() ? null : toBigInteger(g.getT().encode());

		// Switch to affine coordinates.
		switch (g.getCoordinateSystem()) {
			case AFFINE:
				x = gX;
				y = gY;
				break;
			case P2:
			case P3:
				x = gX.multiply(gZ.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				y = gY.multiply(gZ.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				break;
			case P1xP1:
				x = gX.multiply(gZ.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				y = gY.multiply(gT.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				break;
			case CACHED:
				x = gX.subtract(gY).multiply(gZ.multiply(new BigInteger("2")).modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				y = gX.add(gY).multiply(gZ.multiply(new BigInteger("2")).modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				break;
			case PRECOMPUTED:
				x = gX.subtract(gY).multiply(new BigInteger("2").modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				y = gX.add(gY).multiply(new BigInteger("2").modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
				break;
			default:
				throw new UnsupportedOperationException();
		}

		// Now back to the desired coordinate system.
		switch (newCoordinateSystem) {
			case AFFINE:
				return Ed25519GroupElement.affine(
						toFieldElement(x),
						toFieldElement(y),
						Ed25519Field.ONE);
			case P2:
				return Ed25519GroupElement.p2(
						toFieldElement(x),
						toFieldElement(y),
						Ed25519Field.ONE);
			case P3:
				return Ed25519GroupElement.p3(
						toFieldElement(x),
						toFieldElement(y),
						Ed25519Field.ONE,
						toFieldElement(x.multiply(y).mod(Ed25519Field.P)));
			case P1xP1:
				return Ed25519GroupElement.p1xp1(
						toFieldElement(x),
						toFieldElement(y),
						Ed25519Field.ONE,
						Ed25519Field.ONE);
			case CACHED:
				return Ed25519GroupElement.cached(
						toFieldElement(y.add(x).mod(Ed25519Field.P)),
						toFieldElement(y.subtract(x).mod(Ed25519Field.P)),
						Ed25519Field.ONE,
						toFieldElement(D.multiply(new BigInteger("2")).multiply(x).multiply(y).mod(Ed25519Field.P)));
			case PRECOMPUTED:
				return Ed25519GroupElement.precomputed(
						toFieldElement(y.add(x).mod(Ed25519Field.P)),
						toFieldElement(y.subtract(x).mod(Ed25519Field.P)),
						toFieldElement(D.multiply(new BigInteger("2")).multiply(x).multiply(y).mod(Ed25519Field.P)));
			default:
				throw new UnsupportedOperationException();
		}
	}

	/**
	 * Adds two group elements and returns the result in P3 coordinate system.
	 * It uses BigInteger arithmetic and the affine coordinate system.
	 * This method is a helper used to test the projective group addition formulas in Ed25519GroupElement.
	 *
	 * @param g1 The first group element.
	 * @param g2 The second group element.
	 * @return The result of the addition.
	 */
	public static Ed25519GroupElement addGroupElements(final Ed25519GroupElement g1, final Ed25519GroupElement g2) {
		// Relying on a special coordinate system of the group elements.
		if ((g1.getCoordinateSystem() != CoordinateSystem.P2 && g1.getCoordinateSystem() != CoordinateSystem.P3) ||
				(g2.getCoordinateSystem() != CoordinateSystem.P2 && g2.getCoordinateSystem() != CoordinateSystem.P3)) {
			throw new IllegalArgumentException("g1 and g2 must have coordinate system P2 or P3");
		}

		// Projective coordinates
		final BigInteger g1X = toBigInteger(g1.getX().encode());
		final BigInteger g1Y = toBigInteger(g1.getY().encode());
		final BigInteger g1Z = toBigInteger(g1.getZ().encode());
		final BigInteger g2X = toBigInteger(g2.getX().encode());
		final BigInteger g2Y = toBigInteger(g2.getY().encode());
		final BigInteger g2Z = toBigInteger(g2.getZ().encode());

		// Affine coordinates
		final BigInteger g1x = g1X.multiply(g1Z.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
		final BigInteger g1y = g1Y.multiply(g1Z.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
		final BigInteger g2x = g2X.multiply(g2Z.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
		final BigInteger g2y = g2Y.multiply(g2Z.modInverse(Ed25519Field.P)).mod(Ed25519Field.P);

		// Addition formula for affine coordinates. The formula is complete in our case.
		//
		// (x3, y3) = (x1, y1) + (x2, y2) where
		//
		// x3 = (x1 * y2 + x2 * y1) / (1 + d * x1 * x2 * y1 * y2) and
		// y3 = (x1 * x2 + y1 * y2) / (1 - d * x1 * x2 * y1 * y2) and
		// d = -121665/121666
		final BigInteger dx1x2y1y2 = D.multiply(g1x).multiply(g2x).multiply(g1y).multiply(g2y).mod(Ed25519Field.P);
		final BigInteger x3 = g1x.multiply(g2y).add(g2x.multiply(g1y))
				.multiply(BigInteger.ONE.add(dx1x2y1y2).modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
		final BigInteger y3 = g1x.multiply(g2x).add(g1y.multiply(g2y))
				.multiply(BigInteger.ONE.subtract(dx1x2y1y2).modInverse(Ed25519Field.P)).mod(Ed25519Field.P);
		final BigInteger t3 = x3.multiply(y3).mod(Ed25519Field.P);

		return Ed25519GroupElement.p3(toFieldElement(x3), toFieldElement(y3), Ed25519Field.ONE, toFieldElement(t3));
	}

	/**
	 * Doubles a group element and returns the result in the P3 coordinate system.
	 * It uses BigInteger arithmetic and the affine coordinate system.
	 * This method is a helper used to test the projective group doubling formula in Ed25519GroupElement.
	 *
	 * @param g The group element.
	 * @return g+g.
	 */
	public static Ed25519GroupElement doubleGroupElement(final Ed25519GroupElement g) {
		return addGroupElements(g, g);
	}

	/**
	 * Scalar multiply the group element by the field element.
	 *
	 * @param g The group element.
	 * @param f The field element.
	 * @return The resulting group element.
	 */
	public static Ed25519GroupElement scalarMultiplyGroupElement(final Ed25519GroupElement g, final Ed25519FieldElement f) {
		final byte[] bytes = f.encode().getRaw();
		Ed25519GroupElement h = Ed25519Group.ZERO_P3;
		for (int i = 254; i >= 0; i--) {
			h = doubleGroupElement(h);
			if (ArrayUtils.getBit(bytes, i) == 1) {
				h = addGroupElements(h, g);
			}
		}

		return h;
	}

	/**
	 * Calculates f1 * g1 - f2 * g2.
	 *
	 * @param g1 The first group element.
	 * @param f1 The first multiplier.
	 * @param g2 The second group element.
	 * @param f2 The second multiplier.
	 * @return The resulting group element.
	 */
	public static Ed25519GroupElement doubleScalarMultiplyGroupElements(
			final Ed25519GroupElement g1,
			final Ed25519FieldElement f1,
			final Ed25519GroupElement g2,
			final Ed25519FieldElement f2) {
		final Ed25519GroupElement h1 = scalarMultiplyGroupElement(g1, f1);
		final Ed25519GroupElement h2 = scalarMultiplyGroupElement(g2, f2);
		return addGroupElements(h1, h2.negate());
	}

	/**
	 * Negates a group element.
	 *
	 * @param g The group element.
	 * @return The negated group element.
	 */
	public static Ed25519GroupElement negateGroupElement(final Ed25519GroupElement g) {
		if (g.getCoordinateSystem() != CoordinateSystem.P3) {
			throw new IllegalArgumentException("g must have coordinate system P3");
		}

		return Ed25519GroupElement.p3(g.getX().negate(), g.getY(), g.getZ(), g.getT().negate());
	}

	/**
	 * Derives the public key from a private key.
	 *
	 * @param privateKey The private key.
	 * @return The public key.
	 */
	public static PublicKey derivePublicKey(final PrivateKey privateKey) {
		final byte[] hash = Hashes.sha3_512(toByteArray(privateKey.getRaw()));
		final byte[] a = Arrays.copyOfRange(hash, 0, 32);
		a[31] &= 0x7F;
		a[31] |= 0x40;
		a[0] &= 0xF8;
		final Ed25519GroupElement pubKey = scalarMultiplyGroupElement(Ed25519Group.BASE_POINT, toFieldElement(toBigInteger(a)));

		return new PublicKey(pubKey.encode().getRaw());
	}

	/**
	 * Creates a signature from a key pair and message.
	 *
	 * @param keyPair The key pair.
	 * @param data The message.
	 * @return The signature.
	 */
	public static Signature sign(final KeyPair keyPair, final byte[] data) {
		final byte[] hash = Hashes.sha3_512(toByteArray(keyPair.getPrivateKey().getRaw()));
		final byte[] a = Arrays.copyOfRange(hash, 0, 32);
		a[31] &= 0x7F;
		a[31] |= 0x40;
		a[0] &= 0xF8;
		final Ed25519EncodedFieldElement r = new Ed25519EncodedFieldElement(Hashes.sha3_512(
				Arrays.copyOfRange(hash, 32, 64),
				data));
		final Ed25519EncodedFieldElement rReduced = reduceModGroupOrder(r);
		final Ed25519GroupElement R = scalarMultiplyGroupElement(Ed25519Group.BASE_POINT, toFieldElement(toBigInteger(rReduced)));
		final Ed25519EncodedFieldElement h = new Ed25519EncodedFieldElement(Hashes.sha3_512(
				R.encode().getRaw(),
				keyPair.getPublicKey().getRaw(),
				data));
		final Ed25519EncodedFieldElement hReduced = reduceModGroupOrder(h);
		final BigInteger S = toBigInteger(rReduced).add(toBigInteger(hReduced).multiply(toBigInteger(a))).mod(Ed25519Group.GROUP_ORDER);

		return new Signature(R.encode().getRaw(), toByteArray(S));
	}
}

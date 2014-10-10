package org.nem.core.crypto.ed25519.arithmetic;

import org.nem.core.utils.ByteUtils;

import java.io.Serializable;

/**
 * A point on the ED25519 curve which represents a group element.
 * This implementation is based on the ref10 implementation of SUPERCOP.
 *
 * Literature:
 * [1] Daniel J. Bernstein, Niels Duif, Tanja Lange, Peter Schwabe and Bo-Yin Yang : High-speed high-security signatures
 * [2] Huseyin Hisil, Kenneth Koon-Ho Wong, Gary Carter, Ed Dawson: Twisted Edwards Curves Revisited
 * [3] Daniel J. Bernsteina, Tanja Lange: A complete set of addition laws for incomplete Edwards curves
 * [4] Daniel J. Bernstein, Peter Birkner, Marc Joye, Tanja Lange and Christiane Peters: Twisted Edwards Curves
 * [5] Christiane Pascale Peters: Curves, Codes, and Cryptography (PhD thesis)
 * [6] Daniel J. Bernstein, Peter Birkner, Tanja Lange and Christiane Peters: Optimizing double-base elliptic-curve single-scalar multiplication
 */
public class Ed25519GroupElement implements Serializable {

	/**
	 * Available representations for a group element.
	 *
	 * AFFINE: Affine representation (x, y).
	 * P2: Projective representation (X:Y:Z) satisfying x=X/Z, y=Y/Z.
	 * P3: Extended projective representation (X:Y:Z:T) satisfying x=X/Z, y=Y/Z, XY=ZT.
	 * P1P1: Completed representation ((X:Z), (Y:T)) satisfying x=X/Z, y=Y/T.
	 * PRECOMP: Precomputed representation (y+x, y-x, 2dxy).
	 * CACHED: Cached representation (Y+X, Y-X, Z, 2dT)
	 */
    public enum Representation {
		AFFINE,
		P2,
        P3,
        P1P1,
        PRECOMP,
        CACHED
    }

	private final Representation repr;
	private final Ed25519FieldElement X;
	private final Ed25519FieldElement Y;
	private final Ed25519FieldElement Z;
	private final Ed25519FieldElement T;

	/**
	 * Precomputed table for a single scalar multiplication.
	 */
	private Ed25519GroupElement[][] precomputedForSingle;

	/**
	 * Precomputed table for a double scalar multiplication
	 */
	Ed25519GroupElement[] precomputedForDouble;

	/**
	 * Creates a new group element in AFFINE representation.
	 *
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param Z The Z coordinate.
	 * @return The group element in P2 representation.
	 */
	public static Ed25519GroupElement affine(
			final Ed25519FieldElement x,
			final Ed25519FieldElement y,
			final Ed25519FieldElement Z) {
		return new Ed25519GroupElement(Representation.AFFINE, x, y, Z, null);
	}

	/**
	 * Creates a new group element in P2 representation.
	 *
	 * @param X The X coordinate.
	 * @param Y The Y coordinate.
	 * @param Z The Z coordinate.
	 * @return The group element in P2 representation.
	 */
	public static Ed25519GroupElement p2(
			final Ed25519FieldElement X,
			final Ed25519FieldElement Y,
			final Ed25519FieldElement Z) {
		return new Ed25519GroupElement(Representation.P2, X, Y, Z, null);
	}

	/**
	 * Creates a new group element in P3 representation.
	 *
	 * @param X The X coordinate.
	 * @param Y The Y coordinate.
	 * @param Z The Z coordinate.
	 * @param T The T coordinate.
	 * @return The group element in P3 representation.
	 */
    public static Ed25519GroupElement p3(
			final Ed25519FieldElement X,
			final Ed25519FieldElement Y,
			final Ed25519FieldElement Z,
			final Ed25519FieldElement T) {
        return new Ed25519GroupElement(Representation.P3, X, Y, Z, T);
    }

	/**
	 * Creates a new group element in P1P1 representation.
	 *
	 * @param X The X coordinate.
	 * @param Y The Y coordinate.
	 * @param Z The Z coordinate.
	 * @param T The T coordinate.
	 * @return The group element in P1P1 representation.
	 */
	public static Ed25519GroupElement p1p1(
			final Ed25519FieldElement X,
			final Ed25519FieldElement Y,
			final Ed25519FieldElement Z,
			final Ed25519FieldElement T) {
        return new Ed25519GroupElement(Representation.P1P1, X, Y, Z, T);
    }

	/**
	 * Creates a new group element in PRECOMP representation.
	 *
	 * @param ypx The y + x value.
	 * @param ymx The y - x value.
	 * @param xy2d The 2 * d * x * y value.
	 * @return The group element in PRECOMP representation.
	 */
    public static Ed25519GroupElement precomp(
			final Ed25519FieldElement ypx,
			final Ed25519FieldElement ymx,
			final Ed25519FieldElement xy2d) {
        return new Ed25519GroupElement(Representation.PRECOMP, ypx, ymx, xy2d, null);
    }

	/**
	 * Creates a new group element in CACHED representation.
	 *
	 * @param YpX The Y + X value.
	 * @param YmX The Y - X value.
	 * @param Z The Z coordinate.
	 * @param T2d The 2 * d * T value.
	 * @return The group element in CACHED representation.
	 */
    public static Ed25519GroupElement cached(
			final Ed25519FieldElement YpX,
			final Ed25519FieldElement YmX,
			final Ed25519FieldElement Z,
			final Ed25519FieldElement T2d) {
        return new Ed25519GroupElement(Representation.CACHED, YpX, YmX, Z, T2d);
    }

	/**
	 * Creates a group element for a curve.
	 *
	 * @param repr The representation used to represent the group element.
	 * @param X The X coordinate.
	 * @param Y The Y coordinate.
	 * @param Z The Z coordinate.
	 * @param T The T coordinate.
	 */
    public Ed25519GroupElement(
			final Representation repr,
			final Ed25519FieldElement X,
			final Ed25519FieldElement Y,
			final Ed25519FieldElement Z,
			final Ed25519FieldElement T) {
        this.repr = repr;
        this.X = X;
        this.Y = Y;
        this.Z = Z;
        this.T = T;
    }

	/**
	 * Gets the representation of the group element.
	 *
	 * @return The representation.
	 */
	public Representation getRepresentation() {
		return this.repr;
	}

	/**
	 * Gets the X value of the group element.
	 * This is for most representation the projective X coordinate.
	 *
	 * @return The X value.
	 */
	public Ed25519FieldElement getX() {
		return this.X;
	}

	/**
	 * Gets the Y value of the group element.
	 * This is for most representation the projective Y coordinate.
	 *
	 * @return The Y value.
	 */
	public Ed25519FieldElement getY() {
		return this.Y;
	}

	/**
	 * Gets the Z value of the group element.
	 * This is for most representation the projective Z coordinate.
	 *
	 * @return The Z value.
	 */
	public Ed25519FieldElement getZ() {
		return this.Z;
	}

	/**
	 * Gets the T value of the group element.
	 * This is for most representation the projective T coordinate.
	 *
	 * @return The T value.
	 */
	public Ed25519FieldElement getT() {
		return this.T;
	}

	/**
	 * Gets a value indicating whether or not the group element has a
	 * precomputed table for double scalar multiplication.
	 *
	 * @return true if it has the table, false otherwise.
	 */
	public boolean isPrecomputedForDoubleScalarMultiplication() {
		return null != this.precomputedForDouble;
	}

	/**
	 * Gets the table with the precomputed group elements for single scalar multiplication.
	 *
	 * @return The precomputed table.
	 */
	public Ed25519GroupElement[][] getPrecomputedForSingle() {
		return this.precomputedForSingle;
	}

	/**
	 * Gets the table with the precomputed group elements for double scalar multiplication.
	 *
	 * @return The precomputed table.
	 */
	public Ed25519GroupElement[] getPrecomputedForDouble() {
		return this.precomputedForDouble;
	}

	/**
	 * Converts the group element to an encoded point on the curve.
	 *
	 * @return The encoded point as byte array.
	 */
    public Ed25519EncodedGroupElement encode() {
        switch (this.repr) {
			case P2:
			case P3:
				Ed25519FieldElement recip = Z.invert();
				Ed25519FieldElement x = X.multiply(recip);
				Ed25519FieldElement y = Y.multiply(recip);
				byte[] s = y.encode().getRaw();
				s[s.length-1] |= (x.isNegative() ? (byte) 0x80 : 0);
				return new Ed25519EncodedGroupElement(s);
			default:
				return toP2().encode();
        }
    }

	/**
	 * Converts the group element to the P2 representation.
	 *
	 * @return The group element in the P2 representation.
	 */
    public Ed25519GroupElement toP2() {
        return toRep(Representation.P2);
    }

	/**
	 * Converts the group element to the P3 representation.
	 *
	 * @return The group element in the P3 representation.
	 */
    public Ed25519GroupElement toP3() {
        return toRep(Representation.P3);
    }

	/**
	 * Converts the group element to the CACHED representation.
	 *
	 * @return The group element in the CACHED representation.
	 */
    public Ed25519GroupElement toCached() {
        return toRep(Representation.CACHED);
    }

    /**
     * Convert a Ed25519GroupElement from one Representation to another.
	 * TODO-CR: Add additional conversion?
     *
     * Supported conversions:
     * - P3 -> P2
     * - P3 -> CACHED (1 multiply, 1 add, 1 subtract)
     * - P1P1 -> P2 (3 multiply)
     * - P1P1 -> P3 (4 multiply)
	 *
     * @param repr The representation to convert to.
     * @return A new group element in the given representation.
     */
    private Ed25519GroupElement toRep(final Representation repr) {
        switch (this.repr) {
			case P2:
				switch (repr) {
					case P2:
						return p2(this.X, this.Y, this.Z);
					default:
						throw new IllegalArgumentException();
				}
			case P3:
				switch (repr) {
					case P2:
						return p2(this.X, this.Y, this.Z);
					case P3:
						return p3(this.X, this.Y, this.Z, this.T);
					case CACHED:
						return cached(this.Y.add(this.X), this.Y.subtract(this.X), this.Z, this.T.multiply(Ed25519Field.D_Times_TWO));
					default:
						throw new IllegalArgumentException();
				}
			case P1P1:
				switch (repr) {
					case P2:
						return p2(this.X.multiply(this.T), Y.multiply(this.Z), this.Z.multiply(this.T));
					case P3:
						return p3(this.X.multiply(this.T), Y.multiply(this.Z), this.Z.multiply(this.T), this.X.multiply(this.Y));
					case P1P1:
						return p1p1(this.X, this.Y, this.Z, this.T);
					default:
						throw new IllegalArgumentException();
				}
			case PRECOMP:
				switch (repr) {
					case PRECOMP:
						return precomp(this.X, this.Y, this.Z);
					default:
						throw new IllegalArgumentException();
				}
			case CACHED:
				switch (repr) {
					case CACHED:
						return cached(this.X, this.Y, this.Z, this.T);
					default:
						throw new IllegalArgumentException();
				}
			default:
				throw new UnsupportedOperationException();
        }
    }

	/**
	 * Precomputes the group elements needed to speed up a scalar multiplication.
	 */
	public void precomputeForScalarMultiplication() {
		if (null != this.precomputedForSingle) {
			return;
		}
		Ed25519GroupElement Bi = this;
		this.precomputedForSingle = new Ed25519GroupElement[32][8];

		for (int i = 0; i < 32; i++) {
			Ed25519GroupElement Bij = Bi;
			for (int j = 0; j < 8; j++) {
				final Ed25519FieldElement recip = Bij.Z.invert();
				final Ed25519FieldElement x = Bij.X.multiply(recip);
				final Ed25519FieldElement y = Bij.Y.multiply(recip);
				this.precomputedForSingle[i][j] = precomp(y.add(x), y.subtract(x), x.multiply(y).multiply(Ed25519Field.D_Times_TWO));
				Bij = Bij.add(Bi.toCached()).toP3();
			}
			// Only every second summand is precomputed (16^2 = 256).
			for (int k = 0; k < 8; k++) {
				Bi = Bi.add(Bi.toCached()).toP3();
			}
		}
	}

	/**
	 * Precomputes the group elements used to speed up a double scalar multiplication.
	 */
	public void precomputeForDoubleScalarMultiplication() {
		if (null != this.precomputedForDouble) {
			return;
		}
		Ed25519GroupElement Bi = this;
		this.precomputedForDouble = new Ed25519GroupElement[8];
		for (int i = 0; i < 8; i++) {
			final Ed25519FieldElement recip = Bi.Z.invert();
			final Ed25519FieldElement x = Bi.X.multiply(recip);
			final Ed25519FieldElement y = Bi.Y.multiply(recip);
			this.precomputedForDouble[i] = precomp(y.add(x), y.subtract(x), x.multiply(y).multiply(Ed25519Field.D_Times_TWO));
			Bi = this.add(this.add(Bi.toCached()).toP3().toCached()).toP3();
		}
	}

    /**
	 * Doubles a given group element p in P^2 or P^3 representation and returns the result in P x P representation.
	 * r = 2 * p where p = (X : Y : Z) or p = (X : Y : Z : T)
	 *
	 * r in P x P representation:
	 *
	 * r = ((X' : Z'), (Y' : T')) where
	 * X' = (X + Y)^2 - (Y^2 + X^2)
	 * Y' = Y^2 + X^2
	 * Z' = y^2 - X^2
	 * T' = 2 * Z^2 - (y^2 - X^2)
	 *
	 * r converted from P x P to P^2 representation:
	 *
	 * r = (X'' : Y'' : Z'') where
	 * X'' = X' * Z' = ((X + Y)^2 - Y^2 - X^2) * (2 * Z^2 - (y^2 - X^2))
	 * Y'' = Y' * T' = (Y^2 + X^2) * (2 * Z^2 - (y^2 - X^2))
	 * Z'' = Z' * T' = (y^2 - X^2) * (2 * Z^2 - (y^2 - X^2))
	 *
	 * Formula for the P^2 representation is in agreement with the formula given in [4] page 12 (with a = -1)
	 * up to a common factor -1 which does not matter:
	 *
	 * B = (X + Y)^2; C = X^2; D = Y^2; E = -C = -X^2; F := E + D = Y^2 - X^2; H = Z^2; J = F − 2 * H;
	 * X3 = (B − C − D) · J = X' * (-T');
	 * Y3 = F · (E − D) = Z' * (-Y');
	 * Z3 = F · J = Z' * (-T').
	 *
     * @return The P1P1 representation
     */
    public Ed25519GroupElement dbl() {
        switch (this.repr) {
			case P2:
			case P3:
				Ed25519FieldElement XX, YY, B, A, AA, Yn, Zn;
				XX = this.X.square();
				YY = this.Y.square();
				B = this.Z.squareAndDouble();
				A = this.X.add(this.Y);
				AA = A.square();
				Yn = YY.add(XX);
				Zn = YY.subtract(XX);
				return p1p1(AA.subtract(Yn), Yn, Zn, B.subtract(Zn));
			default:
				throw new UnsupportedOperationException();
        }
    }

    /**
     * Ed25519GroupElement addition using the twisted Edwards addition law with
     * extended coordinates (Hisil2008).
	 * this must be in P^3 representation and q in PRECOMP representation.
     * r = p + q where p = this = (X1 : Y1 : Z1 : T1), q = (q.X, q.Y, q.Z) = (Y2/Z2 + X2/Z2, Y2/Z2 - X2/Z2, 2 * d * X2/Z2 * Y2/Z2)
	 *
	 * r in P x P representation:
	 *
	 * r = ((X' : Z'), (Y' : T')) where
	 * X' = (Y1 + X1) * q.X - (Y1 - X1) * q.Y = ((Y1 + X1) * (Y2 + X2) - (Y1 - X1) * (Y2 - X2)) * 1/Z2
	 * Y' = (Y1 + X1) * q.X + (Y1 - X1) * q.Y = ((Y1 + X1) * (Y2 + X2) + (Y1 - X1) * (Y2 - X2)) * 1/Z2
	 * Z' = 2 * Z1 + T1 * q.Z = 2 * Z1 + T1 * 2 * d * X2 * Y2 * 1/Z2^2 = (2 * Z1 * Z2 + 2 * d * T1 * T2) * 1/Z2
	 * T' = 2 * Z1 - T1 * q.Z = 2 * Z1 - T1 * 2 * d * X2 * Y2 * 1/Z2^2 = (2 * Z1 * Z2 - 2 * d * T1 * T2) * 1/Z2
	 *
	 * Setting A = (Y1 - X1) * (Y2 - X2), B = (Y1 + X1) * (Y2 + X2), C = 2 * d * T1 * T2, D = 2 * Z1 * Z2 we get
	 * X' = (B - A) * 1/Z2
	 * Y' = (B + A) * 1/Z2
	 * Z' = (D + C) * 1/Z2
	 * T' = (D - C) * 1/Z2
	 *
	 * r converted from P x P to P^2 representation:
	 *
	 * r = (X'' : Y'' : Z'' : T'') where
	 * X'' = X' * Z' = (B - A) * (D + C) * 1/Z2^2
	 * Y'' = Y' * T' = (B + A) * (D - C) * 1/Z2^2
	 * Z'' = Z' * T' = (D + C) * (D - C) * 1/Z2^2
	 * T'' = X' * Y' = (B - A) * (B + A) * 1/Z2^2
	 *
	 * TODO-CR BR: Formula for the P^2 representation is not in agreement with the formula given in [2] page 6
	 * TODO-CR BR: (the common factor 1/Z2^2 does not matter):
	 * E = B - A, F = D - C, G = D + C, H = B + A
	 * X3 = E * F = (B - A) * (D - C);
	 * Y3 = G * H = (D + C) * (B + A);
	 * Z3 = F * G = (D - C) * (D + C);
	 * T3 = E * H = (B - A) * (B + A);
	 *
     * @param q the PRECOMP representation of the Ed25519GroupElement to add.
     * @return the P1P1 representation of the result.
     */
    private Ed25519GroupElement madd(Ed25519GroupElement q) {
        if (this.repr != Representation.P3) {
			throw new UnsupportedOperationException();
		}
        if (q.repr != Representation.PRECOMP) {
			throw new IllegalArgumentException();
		}

        Ed25519FieldElement YpX, YmX, A, B, C, D;
        YpX = this.Y.add(this.X);
        YmX = this.Y.subtract(this.X);
        A = YpX.multiply(q.X);
        B = YmX.multiply(q.Y);
        C = q.Z.multiply(this.T);
        D = this.Z.add(this.Z);
        return p1p1(A.subtract(B), A.add(B), D.add(C), D.subtract(C));
    }

    /**
     * Ed25519GroupElement subtraction using the twisted Edwards addition law with
     * extended coordinates (Hisil2008).
	 * this must be in P^3 representation and q in PRECOMP representation.
	 * r = p - q where p = this = (X1 : Y1 : Z1 : T1), q = (q.X, q.Y, q.Z) = (Y2/Z2 + X2/Z2, Y2/Z2 - X2/Z2, 2 * d * X2/Z2 * Y2/Z2)
	 *
	 * Negating q means negating the value of X2 and T2 (the latter is irrelevant here).
	 * The formula is in accordance to the above addition.
	 *
     * @param q the PRECOMP representation of the Ed25519GroupElement to subtract.
     * @return the P1P1 representation of the result.
     */
    private Ed25519GroupElement msub(Ed25519GroupElement q) {
        if (this.repr != Representation.P3) {
			throw new UnsupportedOperationException();
		}
        if (q.repr != Representation.PRECOMP) {
			throw new IllegalArgumentException();
		}

        Ed25519FieldElement YpX, YmX, A, B, C, D;
        YpX = this.Y.add(this.X);
        YmX = this.Y.subtract(this.X);
        A = YpX.multiply(q.Y);
        B = YmX.multiply(q.X);
        C = q.Z.multiply(this.T);
        D = this.Z.add(this.Z);
        return p1p1(A.subtract(B), A.add(B), D.subtract(C), D.add(C));
    }

    /**
     * Ed25519GroupElement addition using the twisted Edwards addition law with
     * extended coordinates (Hisil2008).
	 * this must be in P^3 representation and q in CACHED representation.
	 * r = p + q where p = this = (X1 : Y1 : Z1 : T1), q = (q.X, q.Y, q.Z, q.T) = (Y2 + X2, Y2 - X2, Z2, 2 * d * T2)
	 *
	 * r in P x P representation:
	 * X' = (Y1 + X1) * (Y2 + X2) - (Y1 - X1) * (Y2 - X2)
	 * Y' = (Y1 + X1) * (Y2 + X2) + (Y1 - X1) * (Y2 - X2)
	 * Z' = 2 * Z1 * Z2 + 2 * d * T1 * T2
	 * T' = 2 * Z1 * T2 - 2 * d * T1 * T2
	 *
	 * Setting A = (Y1 - X1) * (Y2 - X2), B = (Y1 + X1) * (Y2 + X2), C = 2 * d * T1 * T2, D = 2 * Z1 * Z2 we get
	 * X' = (B - A)
	 * Y' = (B + A)
	 * Z' = (D + C)
	 * T' = (D - C)
	 *
	 * Same result as in madd() (up to a common factor which does not matter).
	 *
	 * @param q the CACHED representation of the Ed25519GroupElement to add.
     * @return the P1P1 representation of the result.
     */
    public Ed25519GroupElement add(Ed25519GroupElement q) {
        if (this.repr != Representation.P3) {
			throw new UnsupportedOperationException();
		}
        if (q.repr != Representation.CACHED) {
			throw new IllegalArgumentException();
		}

        Ed25519FieldElement YpX, YmX, A, B, C, ZZ, D;
        YpX = this.Y.add(this.X);
        YmX = this.Y.subtract(this.X);
        A = YpX.multiply(q.X);
        B = YmX.multiply(q.Y);
        C = q.T.multiply(this.T);
        ZZ = this.Z.multiply(q.Z);
        D = ZZ.add(ZZ);
        return p1p1(A.subtract(B), A.add(B), D.add(C), D.subtract(C));
    }

    /**
     * Ed25519GroupElement subtraction using the twisted Edwards addition law with
     * extended coordinates (Hisil2008).
 	 *
	 * Negating q means negating the value of the coordinate X2 and T2.
	 * The formula is in accordance to the above addition.
	 *
     * @param q the PRECOMP representation of the Ed25519GroupElement to subtract.
     * @return the P1P1 representation of the result.
     */
    public Ed25519GroupElement sub(Ed25519GroupElement q) {
        if (this.repr != Representation.P3) {
			throw new UnsupportedOperationException();
		}
        if (q.repr != Representation.CACHED) {
			throw new IllegalArgumentException();
		}

        Ed25519FieldElement YpX, YmX, A, B, C, ZZ, D;
        YpX = Y.add(X);
        YmX = Y.subtract(X);
        A = YpX.multiply(q.Y);
        B = YmX.multiply(q.X);
        C = q.T.multiply(T);
        ZZ = Z.multiply(q.Z);
        D = ZZ.add(ZZ);
        return p1p1(A.subtract(B), A.add(B), D.subtract(C), D.add(C));
    }

	/**
	 * Negates this group element by subtracting it from the neutral group element.
	 * TODO-CR BR: why not simply negate the coordinates X and T?
	 *
	 * @return The negative of this group element.
	 */
    public Ed25519GroupElement negate() {
        if (this.repr != Representation.P3) {
			throw new UnsupportedOperationException();
		}

        return Ed25519Group.ZERO_P3.sub(this.toCached()).toP3();
    }

    @Override
    public int hashCode() {
        return this.encode().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Ed25519GroupElement)) {
			return false;
		}
        Ed25519GroupElement ge = (Ed25519GroupElement) obj;
        if (!this.repr.equals(ge.repr)) {
            try {
                ge = ge.toRep(this.repr);
            } catch (Exception e) {
                return false;
            }
        }
        switch (this.repr) {
			case P2:
			case P3:
				if (this.Z.equals(ge.Z)) {
					return this.X.equals(ge.X) && this.Y.equals(ge.Y);
				}
				final Ed25519FieldElement x1 = this.X.multiply(ge.Z);
				final Ed25519FieldElement y1 = this.Y.multiply(ge.Z);
				final Ed25519FieldElement x2 = ge.X.multiply(this.Z);
				final Ed25519FieldElement y2 = ge.Y.multiply(this.Z);
				return x1.equals(x2) && y1.equals(y2);
			case P1P1:
				return toP2().equals(ge);
			case PRECOMP:
				return this.X.equals(ge.X) && this.Y.equals(ge.Y) && this.Z.equals(ge.Z);
			case CACHED:
				if (this.Z.equals(ge.Z)) {
					return this.X.equals(ge.X) && this.Y.equals(ge.Y) && this.T.equals(ge.T);
				}
				final Ed25519FieldElement x3 = this.X.multiply(ge.Z);
				final Ed25519FieldElement y3 = this.Y.multiply(ge.Z);
				final Ed25519FieldElement t3 = this.T.multiply(ge.Z);
				final Ed25519FieldElement x4 = ge.X.multiply(this.Z);
				final Ed25519FieldElement y4 = ge.Y.multiply(this.Z);
				final Ed25519FieldElement t4 = ge.T.multiply(this.Z);
				return x3.equals(x4) && y3.equals(y4) && t3.equals(t4);
			default:
				return false;
        }
    }

    /**
     * Convert a to 2^16 representation.
     *
     * @param encoded The encode field element.
     * @return 64 bytes, each between -8 and 7
     */
    private static byte[] toRadix16(final Ed25519EncodedFieldElement encoded) {
		final byte[] a = encoded.getRaw();
		final byte[] e = new byte[64];
		int i;
		for (i = 0; i < 32; i++) {
			e[2*i+0] = (byte) (a[i] & 15);
			e[2*i+1] = (byte) ((a[i] >> 4) & 15);
		}
		/* each e[i] is between 0 and 15 */
		/* e[63] is between 0 and 7 */
		int carry = 0;
		for (i = 0; i < 63; i++) {
			e[i] += carry;
			carry = e[i] + 8;
			carry >>= 4;
			e[i] -= carry << 4;
		}
		e[63] += carry;

		return e;
    }

    /**
     * Constant-time conditional move.
     * Replaces this with u if b == 1.
     * Replaces this with this if b == 0.
     *
     * @param u The group element to return if b == 1.
     * @param b in {0, 1}
     * @return u if b == 1; this if b == 0; null otherwise.
     */
	private Ed25519GroupElement cmov(final Ed25519GroupElement u, final int b) {
        Ed25519GroupElement ret = null;
        for (int i = 0; i < b; i++) {
            // Only for b == 1
            ret = u;
        }
        for (int i = 0; i < 1-b; i++) {
            // Only for b == 0
            ret = this;
        }
        return ret;
    }

    /**
     * Look up 16^i r_i B in the precomputed table.
     * No secret array indices, no secret branching.
     * Constant time.
     *
     * Must have previously precomputed.
     *
     * @param pos = i/2 for i in {0, 2, 4,..., 62}
     * @param b = r_i
     * @return The Ed25519GroupElement
     */
	private Ed25519GroupElement select(final int pos, final int b) {
        // Is r_i negative?
		final int bNegative = ByteUtils.isNegative(b);
        // |r_i|
		final int bAbs = b - (((-bNegative) & b) << 1);

        // 16^i |r_i| B
		final Ed25519GroupElement t = Ed25519Group.ZERO_PRECOMP
                .cmov(this.precomputedForSingle[pos][0], ByteUtils.isEqual(bAbs, 1))
                .cmov(this.precomputedForSingle[pos][1], ByteUtils.isEqual(bAbs, 2))
                .cmov(this.precomputedForSingle[pos][2], ByteUtils.isEqual(bAbs, 3))
                .cmov(this.precomputedForSingle[pos][3], ByteUtils.isEqual(bAbs, 4))
                .cmov(this.precomputedForSingle[pos][4], ByteUtils.isEqual(bAbs, 5))
                .cmov(this.precomputedForSingle[pos][5], ByteUtils.isEqual(bAbs, 6))
                .cmov(this.precomputedForSingle[pos][6], ByteUtils.isEqual(bAbs, 7))
                .cmov(this.precomputedForSingle[pos][7], ByteUtils.isEqual(bAbs, 8));
        // -16^i |r_i| B
		final Ed25519GroupElement tMinus = precomp(t.Y, t.X, t.Z.negate());
        // 16^i r_i B
        return t.cmov(tMinus, bNegative);
    }

    /**
     * h = a * B where a = a[0]+256*a[1]+...+256^31 a[31] and
     * B is this point. If its lookup table has not been precomputed, it
     * will be at the start of the method (and cached for later calls). 
     * Constant time.
     *
     * Preconditions: (TODO: Check this applies here)
     *   a[31] <= 127
     * @param a = a[0]+256*a[1]+...+256^31 a[31]
     * @return the Ed25519GroupElement
     */
    public Ed25519GroupElement scalarMultiply(final Ed25519EncodedFieldElement a) {
        Ed25519GroupElement t;
        int i;

		final byte[] e = toRadix16(a);

        Ed25519GroupElement h = Ed25519Group.ZERO_P3;
        synchronized(this) {
            for (i = 1; i < 64; i += 2) {
                t = select(i/2, e[i]);
                h = h.madd(t).toP3();
            }

            h = h.dbl().toP2().dbl().toP2().dbl().toP2().dbl().toP3();

            for (i = 0; i < 64; i += 2) {
                t = select(i/2, e[i]);
                h = h.madd(t).toP3();
            }
        }

        return h;
    }

    /**
     * Calculates a sliding-windows base 2 representation for a given encoded field element a.
	 * To learn more about it see [6] page 8.
	 *
	 * Output: r which satisfies
	 * a = r0 * 2^0 + r1 * 2^1 + ... + r255 * 2^255 with ri in {-15, -13, -11, -9, -7, -5, -3, -1, 0, 1, 3, 5, 7, 9, 11, 13, 15}
     *
     * Method is package private only so that tests run.
     *
	 * @param encoded The encoded field element.
     * @return The byte array r in the above described form.
     */
    static byte[] slide(final Ed25519EncodedFieldElement encoded) {
		final byte[] a = encoded.getRaw();
        final byte[] r = new byte[256];

        // Put each bit of 'a' into a separate byte, 0 or 1
        for (int i = 0; i < 256; ++i) {
            r[i] = (byte) (1 & (a[i >> 3] >> (i & 7)));
        }

		// Note: r[i] will always be odd.
        for (int i = 0; i < 256; ++i) {
            if (r[i] != 0) {
                for (int b = 1; b <= 6 && i + b < 256; ++b) {
					// Accumulate bits if possible
                    if (r[i + b] != 0) {
                        if (r[i] + (r[i + b] << b) <= 15) {
                            r[i] += r[i + b] << b;
                            r[i + b] = 0;
                        } else if (r[i] - (r[i + b] << b) >= -15) {
                            r[i] -= r[i + b] << b;
                            for (int k = i + b; k < 256; ++k) {
                                if (r[k] == 0) {
                                    r[k] = 1;
                                    break;
                                }
                                r[k] = 0;
                            }
                        } else {
							break;
						}
                    }
                }
            }
        }

        return r;
    }

    /**
     * r = b * B - a * A  where
	 * a and b are encoded field elements and
	 * B is this point.
     *
     * A must have been previously precomputed.
     *
     * @param A in P3 representation.
     * @param a = The first encoded field element.
     * @param b = The second encoded field element.
     * @return the Ed25519GroupElement
     */
    public Ed25519GroupElement doubleScalarMultiplyVariableTime(
			final Ed25519GroupElement A,
			final Ed25519EncodedFieldElement a,
			final Ed25519EncodedFieldElement b) {
		final byte[] aSlide = slide(a);
		final byte[] bSlide = slide(b);

        Ed25519GroupElement r = Ed25519Group.ZERO_P2;

        int i;
        for (i = 255; i >= 0; --i) {
            if (aSlide[i] != 0 || bSlide[i] != 0) break;
        }

        synchronized(this) {
            for (; i >= 0; --i) {
                Ed25519GroupElement t = r.dbl();

                if (aSlide[i] > 0) {
                    t = t.toP3().msub(A.precomputedForDouble[aSlide[i] / 2]);
                } else if(aSlide[i] < 0) {
                    t = t.toP3().madd(A.precomputedForDouble[(-aSlide[i]) / 2]);
                }

                if (bSlide[i] > 0) {
                    t = t.toP3().madd(this.precomputedForDouble[bSlide[i]/2]);
                } else if(bSlide[i] < 0) {
                    t = t.toP3().msub(this.precomputedForDouble[(-bSlide[i])/2]);
                }

                r = t.toP2();
            }
        }

        return r;
    }

    /**
     * Verify that the group element satisfies the curve equation.
	 *
     * @return true if the group element satisfies the curve equation, false otherwise.
     */
    public boolean satisfiesCurveEquation() {
		switch (repr) {
			case P2:
			case P3:
				Ed25519FieldElement recip = Z.invert();
				Ed25519FieldElement x = X.multiply(recip);
				Ed25519FieldElement y = Y.multiply(recip);
				Ed25519FieldElement xSquare = x.square();
				Ed25519FieldElement ySquare = y.square();
				Ed25519FieldElement dXSquareYSquare = Ed25519Field.D.multiply(xSquare).multiply(ySquare);
				return Ed25519Field.ONE.add(dXSquareYSquare).add(xSquare).equals(ySquare);

			default:
				return toP2().satisfiesCurveEquation();
		}
    }

    @Override
    public String toString() {
        return String.format(
				"X=%s\nY=%s\nZ=%s\nT=%s\n",
				this.X.toString(),
				this.Y.toString(),
				this.Z.toString(),
				this.T.toString());
    }
}

package org.nem.core.crypto.ed25519.arithmetic;

import org.nem.core.utils.*;

import java.util.Arrays;

/**
 * Class to represent a field element of the finite field p=2^255-19 elements.
 * An element t, entries t[0]...t[9], represents the integer
 * t[0]+2^26 t[1]+2^51 t[2]+2^77 t[3]+2^102 t[4]+...+2^230 t[9].
 * Bounds on each t[i] vary depending on context.
 *
 * Reviewed/commented by Bloody Rookie (nemproject@gmx.de)
 */
public class Ed25519FieldElement {

	private static final byte[] ZERO = new byte[32];
	private final int[] values;

	/**
	 * Creates a field element.
	 *
	 * @param values The 2^25.5 bit representation of the field element.
	 */
    public Ed25519FieldElement(final int[] values) {
        if (values.length != 10) {
			throw new IllegalArgumentException("Invalid 2^25.5 representation");
		}

        this.values = values;
    }

	/**
	 * Gets the underlying int array.
	 *
	 * @return The int array.
	 */
	public int[] getRaw() {
		return this.values;
	}

	/**
	 * Gets a value indicating whether or not the field element is non-zero.
	 *
	 * @return 1 if it is non-zero, 0 otherwise.
	 */
    public boolean isNonZero() {
        final byte[] s = this.encode();
        return ArrayUtils.isEqual(s, ZERO) == 0;
    }

    /**
	 * Adds the given field element to this and returns the result.
     * h = this + g
     *
     * Preconditions:
     *    |this| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
     *    |g| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
     *
     * Postconditions:
     *    |h| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.
	 *
	 * @param g The field element to add.
	 * @return The field element this + val.
     */
    public Ed25519FieldElement add(final Ed25519FieldElement g) {
        int[] gvalues = g.values;
        int[] h = new int[10];
        h[0] = values[0] + gvalues[0];
        h[1] = values[1] + gvalues[1];
        h[2] = values[2] + gvalues[2];
        h[3] = values[3] + gvalues[3];
        h[4] = values[4] + gvalues[4];
        h[5] = values[5] + gvalues[5];
        h[6] = values[6] + gvalues[6];
        h[7] = values[7] + gvalues[7];
        h[8] = values[8] + gvalues[8];
        h[9] = values[9] + gvalues[9];

        return new Ed25519FieldElement(h);
    }

    /**
	 * Subtract the given field element from this and returns the result.
	 * h = this - g
     *
     * Preconditions:
     *    |this| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
     *    |g| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
     *
     * Postconditions:
     *    |h| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.
	 *
	 * @param g The field element to subtract.
	 * @return The field element this - val.
     **/
    public Ed25519FieldElement subtract(final Ed25519FieldElement g) {
        int[] gvalues = g.values;
		int[] h = new int[10];
		h[0] = values[0] - gvalues[0];
		h[1] = values[1] - gvalues[1];
		h[2] = values[2] - gvalues[2];
		h[3] = values[3] - gvalues[3];
		h[4] = values[4] - gvalues[4];
		h[5] = values[5] - gvalues[5];
		h[6] = values[6] - gvalues[6];
		h[7] = values[7] - gvalues[7];
		h[8] = values[8] - gvalues[8];
		h[9] = values[9] - gvalues[9];

        return new Ed25519FieldElement(h);
    }

    /**
	 * Negates this field element and return the result.
     * h = -this
     *
     * Preconditions:
     *    |this| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
     *
     * Postconditions:
     *    |h| bounded by 1.1*2^25,1.1*2^24,1.1*2^25,1.1*2^24,etc.
	 *
	 * @return The field element (-1) * this.
     */
    public Ed25519FieldElement negate() {
        int[] h = new int[10];
        h[0] = -values[0];
        h[1] = -values[1];
        h[2] = -values[2];
        h[3] = -values[3];
        h[4] = -values[4];
        h[5] = -values[5];
        h[6] = -values[6];
        h[7] = -values[7];
        h[8] = -values[8];
        h[9] = -values[9];

        return new Ed25519FieldElement(h);
    }

    /**
	 * Multiplies this field element with the given field element and returns the result.
     * h = this * g
     * 
     * Preconditions:
	 * |this| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.
	 * |g| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.
     * 
     * Postconditions:
	 * |h| bounded by 1.01*2^25,1.01*2^24,1.01*2^25,1.01*2^24,etc.
     *
     * Notes on implementation strategy:
     *
     * Using schoolbook multiplication. Karatsuba would save a little in some
     * cost models.
     *
     * Most multiplications by 2 and 19 are 32-bit precomputations; cheaper than
     * 64-bit postcomputations.
     *
     * There is one remaining multiplication by 19 in the carry chain; one *19
     * precomputation can be merged into this, but the resulting data flow is
     * considerably less clean.
     *
     * There are 12 carries below. 10 of them are 2-way parallelizable and
     * vectorizable. Can get away with 11 carries, but then data flow is much
     * deeper.
     *
     * With tighter constraints on inputs can squeeze carries into int32.
	 *
	 * @param g The field element to multiply.
	 * @return The (reasonably reduced) field element this * val.
	 */
    public Ed25519FieldElement multiply(final Ed25519FieldElement g) {
        int[] gvalues = g.values;
        int f0 = values[0];
        int f1 = values[1];
        int f2 = values[2];
        int f3 = values[3];
        int f4 = values[4];
        int f5 = values[5];
        int f6 = values[6];
        int f7 = values[7];
        int f8 = values[8];
        int f9 = values[9];
        int g0 = gvalues[0];
        int g1 = gvalues[1];
        int g2 = gvalues[2];
        int g3 = gvalues[3];
        int g4 = gvalues[4];
        int g5 = gvalues[5];
        int g6 = gvalues[6];
        int g7 = gvalues[7];
        int g8 = gvalues[8];
        int g9 = gvalues[9];
        int g1_19 = 19 * g1; /* 1.959375*2^29 */
        int g2_19 = 19 * g2; /* 1.959375*2^30; still ok */
        int g3_19 = 19 * g3;
        int g4_19 = 19 * g4;
        int g5_19 = 19 * g5;
        int g6_19 = 19 * g6;
        int g7_19 = 19 * g7;
        int g8_19 = 19 * g8;
        int g9_19 = 19 * g9;
        int f1_2 = 2 * f1;
        int f3_2 = 2 * f3;
        int f5_2 = 2 * f5;
        int f7_2 = 2 * f7;
        int f9_2 = 2 * f9;
        long f0g0    = f0   * (long) g0;
        long f0g1    = f0   * (long) g1;
        long f0g2    = f0   * (long) g2;
        long f0g3    = f0   * (long) g3;
        long f0g4    = f0   * (long) g4;
        long f0g5    = f0   * (long) g5;
        long f0g6    = f0   * (long) g6;
        long f0g7    = f0   * (long) g7;
        long f0g8    = f0   * (long) g8;
        long f0g9    = f0   * (long) g9;
        long f1g0    = f1   * (long) g0;
        long f1g1_2  = f1_2 * (long) g1;
        long f1g2    = f1   * (long) g2;
        long f1g3_2  = f1_2 * (long) g3;
        long f1g4    = f1   * (long) g4;
        long f1g5_2  = f1_2 * (long) g5;
        long f1g6    = f1   * (long) g6;
        long f1g7_2  = f1_2 * (long) g7;
        long f1g8    = f1   * (long) g8;
        long f1g9_38 = f1_2 * (long) g9_19;
        long f2g0    = f2   * (long) g0;
        long f2g1    = f2   * (long) g1;
        long f2g2    = f2   * (long) g2;
        long f2g3    = f2   * (long) g3;
        long f2g4    = f2   * (long) g4;
        long f2g5    = f2   * (long) g5;
        long f2g6    = f2   * (long) g6;
        long f2g7    = f2   * (long) g7;
        long f2g8_19 = f2   * (long) g8_19;
        long f2g9_19 = f2   * (long) g9_19;
        long f3g0    = f3   * (long) g0;
        long f3g1_2  = f3_2 * (long) g1;
        long f3g2    = f3   * (long) g2;
        long f3g3_2  = f3_2 * (long) g3;
        long f3g4    = f3   * (long) g4;
        long f3g5_2  = f3_2 * (long) g5;
        long f3g6    = f3   * (long) g6;
        long f3g7_38 = f3_2 * (long) g7_19;
        long f3g8_19 = f3   * (long) g8_19;
        long f3g9_38 = f3_2 * (long) g9_19;
        long f4g0    = f4   * (long) g0;
        long f4g1    = f4   * (long) g1;
        long f4g2    = f4   * (long) g2;
        long f4g3    = f4   * (long) g3;
        long f4g4    = f4   * (long) g4;
        long f4g5    = f4   * (long) g5;
        long f4g6_19 = f4   * (long) g6_19;
        long f4g7_19 = f4   * (long) g7_19;
        long f4g8_19 = f4   * (long) g8_19;
        long f4g9_19 = f4   * (long) g9_19;
        long f5g0    = f5   * (long) g0;
        long f5g1_2  = f5_2 * (long) g1;
        long f5g2    = f5   * (long) g2;
        long f5g3_2  = f5_2 * (long) g3;
        long f5g4    = f5   * (long) g4;
        long f5g5_38 = f5_2 * (long) g5_19;
        long f5g6_19 = f5   * (long) g6_19;
        long f5g7_38 = f5_2 * (long) g7_19;
        long f5g8_19 = f5   * (long) g8_19;
        long f5g9_38 = f5_2 * (long) g9_19;
        long f6g0    = f6   * (long) g0;
        long f6g1    = f6   * (long) g1;
        long f6g2    = f6   * (long) g2;
        long f6g3    = f6   * (long) g3;
        long f6g4_19 = f6   * (long) g4_19;
        long f6g5_19 = f6   * (long) g5_19;
        long f6g6_19 = f6   * (long) g6_19;
        long f6g7_19 = f6   * (long) g7_19;
        long f6g8_19 = f6   * (long) g8_19;
        long f6g9_19 = f6   * (long) g9_19;
        long f7g0    = f7   * (long) g0;
        long f7g1_2  = f7_2 * (long) g1;
        long f7g2    = f7   * (long) g2;
        long f7g3_38 = f7_2 * (long) g3_19;
        long f7g4_19 = f7   * (long) g4_19;
        long f7g5_38 = f7_2 * (long) g5_19;
        long f7g6_19 = f7   * (long) g6_19;
        long f7g7_38 = f7_2 * (long) g7_19;
        long f7g8_19 = f7   * (long) g8_19;
        long f7g9_38 = f7_2 * (long) g9_19;
        long f8g0    = f8   * (long) g0;
        long f8g1    = f8   * (long) g1;
        long f8g2_19 = f8   * (long) g2_19;
        long f8g3_19 = f8   * (long) g3_19;
        long f8g4_19 = f8   * (long) g4_19;
        long f8g5_19 = f8   * (long) g5_19;
        long f8g6_19 = f8   * (long) g6_19;
        long f8g7_19 = f8   * (long) g7_19;
        long f8g8_19 = f8   * (long) g8_19;
        long f8g9_19 = f8   * (long) g9_19;
        long f9g0    = f9   * (long) g0;
        long f9g1_38 = f9_2 * (long) g1_19;
        long f9g2_19 = f9   * (long) g2_19;
        long f9g3_38 = f9_2 * (long) g3_19;
        long f9g4_19 = f9   * (long) g4_19;
        long f9g5_38 = f9_2 * (long) g5_19;
        long f9g6_19 = f9   * (long) g6_19;
        long f9g7_38 = f9_2 * (long) g7_19;
        long f9g8_19 = f9   * (long) g8_19;
        long f9g9_38 = f9_2 * (long) g9_19;

		/**
		 * Remember: 2^255 congruent 19 modulo p.
		 * h = h0 * 2^0 + h1 * 2^26 + h2 * 2^(26+25) + h3 * 2^(26+25+26) + ... + h9 * 2^(5*26+5*25).
		 * So to get the real number we would have to multiply the coefficients with the corresponding powers of 2.
		 * To get an idea what is going on below, look at the calculation of h0:
		 * h0 is the coefficient to the power 2^0 so it collects (sums) all products that have the power 2^0.
		 * f0 * g0 really is f0 * 2^0 * g0 * 2^0 = (f0 * g0) * 2^0.
		 * f1 * g9 really is f1 * 2^26 * g9 * 2^230 = f1 * g9 * 2^256 = 2 * f1 * g9 * 2^255 congruent 2 * 19 * f1 * g9 * 2^0 modulo p.
		 * f2 * g8 really is f2 * 2^51 * g8 * 2^204 = f2 * g8 * 2^255 congruent 19 * f2 * g8 * 2^0 modulo p.
		 * and so on...
		 */
        long h0 = f0g0 + f1g9_38 + f2g8_19 + f3g7_38 + f4g6_19 + f5g5_38 + f6g4_19 + f7g3_38 + f8g2_19 + f9g1_38;
        long h1 = f0g1 + f1g0    + f2g9_19 + f3g8_19 + f4g7_19 + f5g6_19 + f6g5_19 + f7g4_19 + f8g3_19 + f9g2_19;
        long h2 = f0g2 + f1g1_2  + f2g0    + f3g9_38 + f4g8_19 + f5g7_38 + f6g6_19 + f7g5_38 + f8g4_19 + f9g3_38;
        long h3 = f0g3 + f1g2    + f2g1    + f3g0    + f4g9_19 + f5g8_19 + f6g7_19 + f7g6_19 + f8g5_19 + f9g4_19;
        long h4 = f0g4 + f1g3_2  + f2g2    + f3g1_2  + f4g0    + f5g9_38 + f6g8_19 + f7g7_38 + f8g6_19 + f9g5_38;
        long h5 = f0g5 + f1g4    + f2g3    + f3g2    + f4g1    + f5g0    + f6g9_19 + f7g8_19 + f8g7_19 + f9g6_19;
        long h6 = f0g6 + f1g5_2  + f2g4    + f3g3_2  + f4g2    + f5g1_2  + f6g0    + f7g9_38 + f8g8_19 + f9g7_38;
        long h7 = f0g7 + f1g6    + f2g5    + f3g4    + f4g3    + f5g2    + f6g1    + f7g0    + f8g9_19 + f9g8_19;
        long h8 = f0g8 + f1g7_2  + f2g6    + f3g5_2  + f4g4    + f5g3_2  + f6g2    + f7g1_2  + f8g0    + f9g9_38;
        long h9 = f0g9 + f1g8    + f2g7    + f3g6    + f4g5    + f5g4    + f6g3    + f7g2    + f8g1    + f9g0;
        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        /**
         * |h0| <= (1.65*1.65*2^52*(1+19+19+19+19)+1.65*1.65*2^50*(38+38+38+38+38))
		 * i.e. |h0| <= 1.4*2^60; narrower ranges for h2, h4, h6, h8
		 * |h1| <= (1.65*1.65*2^51*(1+1+19+19+19+19+19+19+19+19))
		 * i.e. |h1| <= 1.7*2^59; narrower ranges for h3, h5, h7, h9
         */

        carry0 = (h0 + (long) (1<<25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1<<25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
        /* |h0| <= 2^25 */
        /* |h4| <= 2^25 */
        /* |h1| <= 1.71*2^59 */
        /* |h5| <= 1.71*2^59 */

        carry1 = (h1 + (long) (1<<24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1<<24)) >> 25; h6 += carry5; h5 -= carry5 << 25;
        /* |h1| <= 2^24; from now on fits into int32 */
        /* |h5| <= 2^24; from now on fits into int32 */
        /* |h2| <= 1.41*2^60 */
        /* |h6| <= 1.41*2^60 */

        carry2 = (h2 + (long) (1<<25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1<<25)) >> 26; h7 += carry6; h6 -= carry6 << 26;
        /* |h2| <= 2^25; from now on fits into int32 unchanged */
        /* |h6| <= 2^25; from now on fits into int32 unchanged */
        /* |h3| <= 1.71*2^59 */
        /* |h7| <= 1.71*2^59 */

        carry3 = (h3 + (long) (1<<24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1<<24)) >> 25; h8 += carry7; h7 -= carry7 << 25;
        /* |h3| <= 2^24; from now on fits into int32 unchanged */
        /* |h7| <= 2^24; from now on fits into int32 unchanged */
        /* |h4| <= 1.72*2^34 */
        /* |h8| <= 1.41*2^60 */

        carry4 = (h4 + (long) (1<<25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1<<25)) >> 26; h9 += carry8; h8 -= carry8 << 26;
        /* |h4| <= 2^25; from now on fits into int32 unchanged */
        /* |h8| <= 2^25; from now on fits into int32 unchanged */
        /* |h5| <= 1.01*2^24 */
        /* |h9| <= 1.71*2^59 */

        carry9 = (h9 + (long) (1<<24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;
        /* |h9| <= 2^24; from now on fits into int32 unchanged */
        /* |h0| <= 1.1*2^39 */

        carry0 = (h0 + (long) (1<<25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
        /* |h0| <= 2^25; from now on fits into int32 unchanged */
        /* |h1| <= 1.01*2^24 */

        int[] h = new int[10];
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;

        return new Ed25519FieldElement(h);
    }

    /**
	 * Squares this field element and returns the result.
     * h = this * this
     *
     * Preconditions:
     *    |this| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.
     *
     * Postconditions:
     *    |h| bounded by 1.01*2^25,1.01*2^24,1.01*2^25,1.01*2^24,etc.
     *
     * See multiply for discussion of implementation strategy.
	 *
	 * @return The square of this field element.
     */
    public Ed25519FieldElement square() {
        int f0 = values[0];
        int f1 = values[1];
        int f2 = values[2];
        int f3 = values[3];
        int f4 = values[4];
        int f5 = values[5];
        int f6 = values[6];
        int f7 = values[7];
        int f8 = values[8];
        int f9 = values[9];
        int f0_2 = 2 * f0;
        int f1_2 = 2 * f1;
        int f2_2 = 2 * f2;
        int f3_2 = 2 * f3;
        int f4_2 = 2 * f4;
        int f5_2 = 2 * f5;
        int f6_2 = 2 * f6;
        int f7_2 = 2 * f7;
        int f5_38 = 38 * f5; /* 1.959375*2^30 */
        int f6_19 = 19 * f6; /* 1.959375*2^30 */
        int f7_38 = 38 * f7; /* 1.959375*2^30 */
        int f8_19 = 19 * f8; /* 1.959375*2^30 */
        int f9_38 = 38 * f9; /* 1.959375*2^30 */
        long f0f0    = f0   * (long) f0;
        long f0f1_2  = f0_2 * (long) f1;
        long f0f2_2  = f0_2 * (long) f2;
        long f0f3_2  = f0_2 * (long) f3;
        long f0f4_2  = f0_2 * (long) f4;
        long f0f5_2  = f0_2 * (long) f5;
        long f0f6_2  = f0_2 * (long) f6;
        long f0f7_2  = f0_2 * (long) f7;
        long f0f8_2  = f0_2 * (long) f8;
        long f0f9_2  = f0_2 * (long) f9;
        long f1f1_2  = f1_2 * (long) f1;
        long f1f2_2  = f1_2 * (long) f2;
        long f1f3_4  = f1_2 * (long) f3_2;
        long f1f4_2  = f1_2 * (long) f4;
        long f1f5_4  = f1_2 * (long) f5_2;
        long f1f6_2  = f1_2 * (long) f6;
        long f1f7_4  = f1_2 * (long) f7_2;
        long f1f8_2  = f1_2 * (long) f8;
        long f1f9_76 = f1_2 * (long) f9_38;
        long f2f2    = f2   * (long) f2;
        long f2f3_2  = f2_2 * (long) f3;
        long f2f4_2  = f2_2 * (long) f4;
        long f2f5_2  = f2_2 * (long) f5;
        long f2f6_2  = f2_2 * (long) f6;
        long f2f7_2  = f2_2 * (long) f7;
        long f2f8_38 = f2_2 * (long) f8_19;
        long f2f9_38 = f2   * (long) f9_38;
        long f3f3_2  = f3_2 * (long) f3;
        long f3f4_2  = f3_2 * (long) f4;
        long f3f5_4  = f3_2 * (long) f5_2;
        long f3f6_2  = f3_2 * (long) f6;
        long f3f7_76 = f3_2 * (long) f7_38;
        long f3f8_38 = f3_2 * (long) f8_19;
        long f3f9_76 = f3_2 * (long) f9_38;
        long f4f4    = f4   * (long) f4;
        long f4f5_2  = f4_2 * (long) f5;
        long f4f6_38 = f4_2 * (long) f6_19;
        long f4f7_38 = f4   * (long) f7_38;
        long f4f8_38 = f4_2 * (long) f8_19;
        long f4f9_38 = f4   * (long) f9_38;
        long f5f5_38 = f5   * (long) f5_38;
        long f5f6_38 = f5_2 * (long) f6_19;
        long f5f7_76 = f5_2 * (long) f7_38;
        long f5f8_38 = f5_2 * (long) f8_19;
        long f5f9_76 = f5_2 * (long) f9_38;
        long f6f6_19 = f6   * (long) f6_19;
        long f6f7_38 = f6   * (long) f7_38;
        long f6f8_38 = f6_2 * (long) f8_19;
        long f6f9_38 = f6   * (long) f9_38;
        long f7f7_38 = f7   * (long) f7_38;
        long f7f8_38 = f7_2 * (long) f8_19;
        long f7f9_76 = f7_2 * (long) f9_38;
        long f8f8_19 = f8   * (long) f8_19;
        long f8f9_38 = f8   * (long) f9_38;
        long f9f9_38 = f9   * (long) f9_38;

		/**
		 * Same procedure as in multiply, but this time we have a higher symmetry leading to less summands.
		 * e.g. f1f9_76 really stands for f1 * 2^26 * f9 * 2^230 + f9 * 2^230 + f1 * 2^26 congruent 2 * 2 * 19 * f1 * f9  2^0 modulo p.
		 */
        long h0 = f0f0   + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
        long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
        long h2 = f0f2_2 + f1f1_2  + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
        long h3 = f0f3_2 + f1f2_2  + f4f9_38 + f5f8_38 + f6f7_38;
        long h4 = f0f4_2 + f1f3_4  + f2f2    + f5f9_76 + f6f8_38 + f7f7_38;
        long h5 = f0f5_2 + f1f4_2  + f2f3_2  + f6f9_38 + f7f8_38;
        long h6 = f0f6_2 + f1f5_4  + f2f4_2  + f3f3_2  + f7f9_76 + f8f8_19;
        long h7 = f0f7_2 + f1f6_2  + f2f5_2  + f3f4_2  + f8f9_38;
        long h8 = f0f8_2 + f1f7_4  + f2f6_2  + f3f5_4  + f4f4    + f9f9_38;
        long h9 = f0f9_2 + f1f8_2  + f2f7_2  + f3f6_2  + f4f5_2;
        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        carry0 = (h0 + (long) (1<<25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1<<25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
        carry1 = (h1 + (long) (1<<24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1<<24)) >> 25; h6 += carry5; h5 -= carry5 << 25;
        carry2 = (h2 + (long) (1<<25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1<<25)) >> 26; h7 += carry6; h6 -= carry6 << 26;
        carry3 = (h3 + (long) (1<<24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1<<24)) >> 25; h8 += carry7; h7 -= carry7 << 25;
        carry4 = (h4 + (long) (1<<25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1<<25)) >> 26; h9 += carry8; h8 -= carry8 << 26;
        carry9 = (h9 + (long) (1<<24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;
        carry0 = (h0 + (long) (1<<25)) >> 26; h1 += carry0; h0 -= carry0 << 26;

        int[] h = new int[10];
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;

        return new Ed25519FieldElement(h);
    }

    /**
	 * Squares this field element, multiplies by two and returns the result.
     * h = 2 * this * this
     *
     * Preconditions:
     *    |this| bounded by 1.65*2^26,1.65*2^25,1.65*2^26,1.65*2^25,etc.
     *
     * Postconditions:
     *    |h| bounded by 1.01*2^25,1.01*2^24,1.01*2^25,1.01*2^24,etc.
     *
     * See multiply for discussion of implementation strategy.
	 *
	 * @return The square of this field element times 2.
     */
    public Ed25519FieldElement squareAndDouble() {
        int f0 = values[0];
        int f1 = values[1];
        int f2 = values[2];
        int f3 = values[3];
        int f4 = values[4];
        int f5 = values[5];
        int f6 = values[6];
        int f7 = values[7];
        int f8 = values[8];
        int f9 = values[9];
        int f0_2 = 2 * f0;
        int f1_2 = 2 * f1;
        int f2_2 = 2 * f2;
        int f3_2 = 2 * f3;
        int f4_2 = 2 * f4;
        int f5_2 = 2 * f5;
        int f6_2 = 2 * f6;
        int f7_2 = 2 * f7;
        int f5_38 = 38 * f5; /* 1.959375*2^30 */
        int f6_19 = 19 * f6; /* 1.959375*2^30 */
        int f7_38 = 38 * f7; /* 1.959375*2^30 */
        int f8_19 = 19 * f8; /* 1.959375*2^30 */
        int f9_38 = 38 * f9; /* 1.959375*2^30 */
        long f0f0    = f0   * (long) f0;
        long f0f1_2  = f0_2 * (long) f1;
        long f0f2_2  = f0_2 * (long) f2;
        long f0f3_2  = f0_2 * (long) f3;
        long f0f4_2  = f0_2 * (long) f4;
        long f0f5_2  = f0_2 * (long) f5;
        long f0f6_2  = f0_2 * (long) f6;
        long f0f7_2  = f0_2 * (long) f7;
        long f0f8_2  = f0_2 * (long) f8;
        long f0f9_2  = f0_2 * (long) f9;
        long f1f1_2  = f1_2 * (long) f1;
        long f1f2_2  = f1_2 * (long) f2;
        long f1f3_4  = f1_2 * (long) f3_2;
        long f1f4_2  = f1_2 * (long) f4;
        long f1f5_4  = f1_2 * (long) f5_2;
        long f1f6_2  = f1_2 * (long) f6;
        long f1f7_4  = f1_2 * (long) f7_2;
        long f1f8_2  = f1_2 * (long) f8;
        long f1f9_76 = f1_2 * (long) f9_38;
        long f2f2    = f2   * (long) f2;
        long f2f3_2  = f2_2 * (long) f3;
        long f2f4_2  = f2_2 * (long) f4;
        long f2f5_2  = f2_2 * (long) f5;
        long f2f6_2  = f2_2 * (long) f6;
        long f2f7_2  = f2_2 * (long) f7;
        long f2f8_38 = f2_2 * (long) f8_19;
        long f2f9_38 = f2   * (long) f9_38;
        long f3f3_2  = f3_2 * (long) f3;
        long f3f4_2  = f3_2 * (long) f4;
        long f3f5_4  = f3_2 * (long) f5_2;
        long f3f6_2  = f3_2 * (long) f6;
        long f3f7_76 = f3_2 * (long) f7_38;
        long f3f8_38 = f3_2 * (long) f8_19;
        long f3f9_76 = f3_2 * (long) f9_38;
        long f4f4    = f4   * (long) f4;
        long f4f5_2  = f4_2 * (long) f5;
        long f4f6_38 = f4_2 * (long) f6_19;
        long f4f7_38 = f4   * (long) f7_38;
        long f4f8_38 = f4_2 * (long) f8_19;
        long f4f9_38 = f4   * (long) f9_38;
        long f5f5_38 = f5   * (long) f5_38;
        long f5f6_38 = f5_2 * (long) f6_19;
        long f5f7_76 = f5_2 * (long) f7_38;
        long f5f8_38 = f5_2 * (long) f8_19;
        long f5f9_76 = f5_2 * (long) f9_38;
        long f6f6_19 = f6   * (long) f6_19;
        long f6f7_38 = f6   * (long) f7_38;
        long f6f8_38 = f6_2 * (long) f8_19;
        long f6f9_38 = f6   * (long) f9_38;
        long f7f7_38 = f7   * (long) f7_38;
        long f7f8_38 = f7_2 * (long) f8_19;
        long f7f9_76 = f7_2 * (long) f9_38;
        long f8f8_19 = f8   * (long) f8_19;
        long f8f9_38 = f8   * (long) f9_38;
        long f9f9_38 = f9   * (long) f9_38;
        long h0 = f0f0   + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
        long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
        long h2 = f0f2_2 + f1f1_2  + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
        long h3 = f0f3_2 + f1f2_2  + f4f9_38 + f5f8_38 + f6f7_38;
        long h4 = f0f4_2 + f1f3_4  + f2f2    + f5f9_76 + f6f8_38 + f7f7_38;
        long h5 = f0f5_2 + f1f4_2  + f2f3_2  + f6f9_38 + f7f8_38;
        long h6 = f0f6_2 + f1f5_4  + f2f4_2  + f3f3_2  + f7f9_76 + f8f8_19;
        long h7 = f0f7_2 + f1f6_2  + f2f5_2  + f3f4_2  + f8f9_38;
        long h8 = f0f8_2 + f1f7_4  + f2f6_2  + f3f5_4  + f4f4    + f9f9_38;
        long h9 = f0f9_2 + f1f8_2  + f2f7_2  + f3f6_2  + f4f5_2;
        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        h0 += h0;
        h1 += h1;
        h2 += h2;
        h3 += h3;
        h4 += h4;
        h5 += h5;
        h6 += h6;
        h7 += h7;
        h8 += h8;
        h9 += h9;

        carry0 = (h0 + (long) (1<<25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1<<25)) >> 26; h5 += carry4; h4 -= carry4 << 26;

        carry1 = (h1 + (long) (1<<24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1<<24)) >> 25; h6 += carry5; h5 -= carry5 << 25;

        carry2 = (h2 + (long) (1<<25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1<<25)) >> 26; h7 += carry6; h6 -= carry6 << 26;

        carry3 = (h3 + (long) (1<<24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1<<24)) >> 25; h8 += carry7; h7 -= carry7 << 25;

        carry4 = (h4 + (long) (1<<25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1<<25)) >> 26; h9 += carry8; h8 -= carry8 << 26;

        carry9 = (h9 + (long) (1<<24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;

        carry0 = (h0 + (long) (1<<25)) >> 26; h1 += carry0; h0 -= carry0 << 26;

        int[] h = new int[10];
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;
        return new Ed25519FieldElement(h);
    }

	/**
	 * Invert this field element and return the result.
	 * The inverse is found via Fermat's little theorem:
	 * a^p congruent a mod p and therefore a^(p-2) congruent a^-1 mod p
	 *
	 * @return The inverse of this field element.
	 */
    public Ed25519FieldElement invert() {
		Ed25519FieldElement f0, f1, f2, f3;

		// 2 == 2 * 1
        f0 = square();

		// 4 == 2 * 2
        f1 = f0.square();

		// 8 == 2 * 4
        for (int i = 1; i < 2; ++i) {
            f1 = f1.square();
        }

		// 9 == 8 + 1
        f1 = multiply(f1);

		// 11 == 9 + 2
        f0 = f0.multiply(f1);

		// 22 == 2 * 11
        f2 = f0.square();

		// 31 == 22 + 9
        f1 = f1.multiply(f2);

		// 2^6 - 2^1
        f2 = f1.square();

		// 2^10 - 2^5
        for (int i = 1; i < 5; ++i) {
            f2 = f2.square();
        }

		// 2^10 - 2^0
        f1 = f2.multiply(f1);

		// 2^11 - 2^1
        f2 = f1.square();

		// 2^20 - 2^10
        for (int i = 1; i < 10; ++i) {
            f2 = f2.square();
        }

		// 2^20 - 2^0
        f2 = f2.multiply(f1);

		// 2^21 - 2^1
        f3 = f2.square();

		// 2^40 - 2^20
        for (int i = 1; i < 20; ++i) {
            f3 = f3.square();
        }

		// 2^40 - 2^0
        f2 = f3.multiply(f2);

		// 2^41 - 2^1
        f2 = f2.square();

		// 2^50 - 2^10
        for (int i = 1; i < 10; ++i) {
            f2 = f2.square();
        }

		// 2^50 - 2^0
        f1 = f2.multiply(f1);

		// 2^51 - 2^1
        f2 = f1.square();

		// 2^100 - 2^50
        for (int i = 1; i < 50; ++i) {
            f2 = f2.square();
        }

		// 2^100 - 2^0
        f2 = f2.multiply(f1);

		// 2^101 - 2^1
        f3 = f2.square();

		// 2^200 - 2^100
        for (int i = 1; i < 100; ++i) {
            f3 = f3.square();
        }

		// 2^200 - 2^0
        f2 = f3.multiply(f2);

		// 2^201 - 2^1
        f2 = f2.square();

		// 2^250 - 2^50
        for (int i = 1; i < 50; ++i) {
            f2 = f2.square();
        }

		// 2^250 - 2^0
        f1 = f2.multiply(f1);

		// 2^251 - 2^1
        f1 = f1.square();

		// 2^255 - 2^5
        for (int i = 1; i < 5; ++i) {
            f1 = f1.square();
        }

		// 2^255 - 21
        return f1.multiply(f0);
    }

	/**
	 * Computes this field element to the power of (2^252 - 3) and returns the result.
	 * This is a helper function for calculating the square root.
	 * TODO-CR BR: I think it makes sense to have a sqrt function.
	 *
	 * @return This field element to the power of (2^252 - 3).
	 */
    public Ed25519FieldElement pow22523() {
		Ed25519FieldElement f0, f1, f2;

		// 2 == 2 * 1
        f0 = square();

		// 4 == 2 * 2
        f1 = f0.square();

		// 8 == 2 * 4
        for (int i = 1; i < 2; ++i) {
            f1 = f1.square();
        }

        // z9 = z1*z8
        f1 = multiply(f1);

		// 11 == 9 + 2
        f0 = f0.multiply(f1);

		// 22 == 2 * 11
        f0 = f0.square();

		// 31 == 22 + 9
        f0 = f1.multiply(f0);

		// 2^6 - 2^1
        f1 = f0.square();

		// 2^10 - 2^5
        for (int i = 1; i < 5; ++i) {
            f1 = f1.square();
        }

		// 2^10 - 2^0
        f0 = f1.multiply(f0);

		// 2^11 - 2^1
        f1 = f0.square();

		// 2^20 - 2^10
        for (int i = 1; i < 10; ++i) {
            f1 = f1.square();
        }

		// 2^20 - 2^0
        f1 = f1.multiply(f0);

		// 2^21 - 2^1
        f2 = f1.square();

		// 2^40 - 2^20
        for (int i = 1; i < 20; ++i) {
            f2 = f2.square();
        }

		// 2^40 - 2^0
        f1 = f2.multiply(f1);

		// 2^41 - 2^1
        f1 = f1.square();

		// 2^50 - 2^10
        for (int i = 1; i < 10; ++i) {
            f1 = f1.square();
        }

		// 2^50 - 2^0
        f0 = f1.multiply(f0);

		// 2^51 - 2^1
        f1 = f0.square();

		// 2^100 - 2^50
        for (int i = 1; i < 50; ++i) {
            f1 = f1.square();
        }

		// 2^100 - 2^0
        f1 = f1.multiply(f0);

		// 2^101 - 2^1
        f2 = f1.square();

		// 2^200 - 2^100
        for (int i = 1; i < 100; ++i) {
            f2 = f2.square();
        }

		// 2^200 - 2^0
        f1 = f2.multiply(f1);

		// 2^201 - 2^1
        f1 = f1.square();

		// 2^250 - 2^50
        for (int i = 1; i < 50; ++i) {
            f1 = f1.square();
        }

		// 2^250 - 2^0
        f0 = f1.multiply(f0);

		// 2^251 - 2^1
        f0 = f0.square();

		// 2^252 - 2^2
        for (int i = 1; i < 2; ++i) {
            f0 = f0.square();
        }

		// 2^252 - 3
        return multiply(f0);
    }

	/**
	 * Reduce this field element modulo field size p = 2^255 - 19 and return the result.
	 *
	 * The idea for the modulo p reduction algorithm is as follows:
	 * Assumption:
	 * p = 2^255 - 19
	 * h = h0 + 2^25 * h1 + 2^(26+25) * h2 + ... + 2^230 * h9 where 0 <= |hi| < 2^27 for all i=0,...,9.
	 * h congruent r modulo p, i.e. h = r + q * p for some suitable 0 <= r < p and an integer q.
	 *
	 * Then q = [2^-255 * (h + 19 * 2^-25 * h9 + 1/2)] where [x] = floor(x).
	 *
	 * Proof:
	 * We begin with some very raw estimation for the bounds of some expressions:
	 * |h| < 2^230 * 2^30 = 2^260 ==> |r + q * p| < 2^260 ==> |q| < 2^10.
	 * ==> -1/4 <= a := 19^2 * 2^-255 * q < 1/4.
	 * |h - 2^230 * h9| = |h0 + ... + 2^204 * h8| < 2^204 * 2^30 = 2^234.
	 * ==> -1/4 <= b := 19 * 2^-255 * (h - 2^230 * h9) < 1/4
	 * Therefore 0 < 1/2 - a - b < 1.
	 *
	 * Set x := r + 19 * 2^-255 * r + 1/2 - a - b then
	 * 0 <= x < 255 - 20 + 19 + 1 = 2^255 ==> 0 <= 2^-255 * x < 1. Since q is an integer we have
	 *
	 * [q + 2^-255 * x] = q        (1)
	 *
	 * Have a closer look at x:
	 * x = h - q * (2^255 - 19) + 19 * 2^-255 * (h - q * (2^255 - 19)) + 1/2 - 19^2 * 2^-255 * q - 19 * 2^-255 * (h - 2^230 * h9)
	 *   = h - q * 2^255 + 19 * q + 19 * 2^-255 * h - 19 * q + 19^2 * 2^-255 * q + 1/2 - 19^2 * 2^-255 * q - 19 * 2^-255 * h + 19 * 2^-25 * h9
	 *   = h + 19 * 2^-25 * h9 + 1/2 - q^255.
	 *
	 * Inserting the expression for x into (1) we get the desired expression for q.
	 *
	 * @return The mod p reduced field element;
	 */
	public Ed25519FieldElement modP() {
		int h0 = this.values[0];
		int h1 = this.values[1];
		int h2 = this.values[2];
		int h3 = this.values[3];
		int h4 = this.values[4];
		int h5 = this.values[5];
		int h6 = this.values[6];
		int h7 = this.values[7];
		int h8 = this.values[8];
		int h9 = this.values[9];
		int q;
		int carry0;
		int carry1;
		int carry2;
		int carry3;
		int carry4;
		int carry5;
		int carry6;
		int carry7;
		int carry8;
		int carry9;

		// Calculate q
		q = (19 * h9 + (1 << 24)) >> 25;
		q = (h0 + q) >> 26;
		q = (h1 + q) >> 25;
		q = (h2 + q) >> 26;
		q = (h3 + q) >> 25;
		q = (h4 + q) >> 26;
		q = (h5 + q) >> 25;
		q = (h6 + q) >> 26;
		q = (h7 + q) >> 25;
		q = (h8 + q) >> 26;
		q = (h9 + q) >> 25;

		// r = h - q * p = h - 2^255 * q + 19 * q
		// First add 19 * q then discard the bit 255
		h0 += 19 * q;

		carry0 = h0 >> 26; h1 += carry0; h0 -= carry0 << 26;
		carry1 = h1 >> 25; h2 += carry1; h1 -= carry1 << 25;
		carry2 = h2 >> 26; h3 += carry2; h2 -= carry2 << 26;
		carry3 = h3 >> 25; h4 += carry3; h3 -= carry3 << 25;
		carry4 = h4 >> 26; h5 += carry4; h4 -= carry4 << 26;
		carry5 = h5 >> 25; h6 += carry5; h5 -= carry5 << 25;
		carry6 = h6 >> 26; h7 += carry6; h6 -= carry6 << 26;
		carry7 = h7 >> 25; h8 += carry7; h7 -= carry7 << 25;
		carry8 = h8 >> 26; h9 += carry8; h8 -= carry8 << 26;
		carry9 = h9 >> 25;               h9 -= carry9 << 25;

		int[] h = new int[10];
		h[0] = h0;
		h[1] = h1;
		h[2] = h2;
		h[3] = h3;
		h[4] = h4;
		h[5] = h5;
		h[6] = h6;
		h[7] = h7;
		h[8] = h8;
		h[9] = h9;

		return new Ed25519FieldElement(h);
	}

	/**
	 * Encodes a given field element in its 32 byte 2^8 bit representation. This is done in two steps.
	 * Step 1: Reduce the value of the field element modulo p.
	 * Step 2: Convert the field element to the 32 byte representation.
	 *
	 */
	public byte[] encode() {
		// Step 1:
		int[] gvalues = this.modP().getRaw();
		int h0 = gvalues[0];
		int h1 = gvalues[1];
		int h2 = gvalues[2];
		int h3 = gvalues[3];
		int h4 = gvalues[4];
		int h5 = gvalues[5];
		int h6 = gvalues[6];
		int h7 = gvalues[7];
		int h8 = gvalues[8];
		int h9 = gvalues[9];

		// Step 2:
		byte[] s = new byte[32];
		s[0] = (byte) (h0 >> 0);
		s[1] = (byte) (h0 >> 8);
		s[2] = (byte) (h0 >> 16);
		s[3] = (byte) ((h0 >> 24) | (h1 << 2));
		s[4] = (byte) (h1 >> 6);
		s[5] = (byte) (h1 >> 14);
		s[6] = (byte) ((h1 >> 22) | (h2 << 3));
		s[7] = (byte) (h2 >> 5);
		s[8] = (byte) (h2 >> 13);
		s[9] = (byte) ((h2 >> 21) | (h3 << 5));
		s[10] = (byte) (h3 >> 3);
		s[11] = (byte) (h3 >> 11);
		s[12] = (byte) ((h3 >> 19) | (h4 << 6));
		s[13] = (byte) (h4 >> 2);
		s[14] = (byte) (h4 >> 10);
		s[15] = (byte) (h4 >> 18);
		s[16] = (byte) (h5 >> 0);
		s[17] = (byte) (h5 >> 8);
		s[18] = (byte) (h5 >> 16);
		s[19] = (byte) ((h5 >> 24) | (h6 << 1));
		s[20] = (byte) (h6 >> 7);
		s[21] = (byte) (h6 >> 15);
		s[22] = (byte) ((h6 >> 23) | (h7 << 3));
		s[23] = (byte) (h7 >> 5);
		s[24] = (byte) (h7 >> 13);
		s[25] = (byte) ((h7 >> 21) | (h8 << 4));
		s[26] = (byte) (h8 >> 4);
		s[27] = (byte) (h8 >> 12);
		s[28] = (byte) ((h8 >> 20) | (h9 << 6));
		s[29] = (byte) (h9 >> 2);
		s[30] = (byte) (h9 >> 10);
		s[31] = (byte) (h9 >> 18);

		return s;
	}

	/**
	 * Decodes an integer in 32 byte 2^8 bit representation to a field element in its 10 byte 2^25.5 representation.
	 *
	 * @param in The 32 byte 2^8 bit representation.
	 * @return The field element in its 2^25.5 bit representation.
	 */
	public static Ed25519FieldElement decode(final byte[] in) {
		long h0 = fourBytesToLong(in, 0);
		long h1 = threeBytesToLong(in, 4) << 6;
		long h2 = threeBytesToLong(in, 7) << 5;
		long h3 = threeBytesToLong(in, 10) << 3;
		long h4 = threeBytesToLong(in, 13) << 2;
		long h5 = threeBytesToLong(in, 16);
		long h6 = threeBytesToLong(in, 20) << 7;
		long h7 = threeBytesToLong(in, 23) << 5;
		long h8 = threeBytesToLong(in, 26) << 4;
		long h9 = (threeBytesToLong(in, 29) & 0x7FFFFF) << 2;
		long carry0;
		long carry1;
		long carry2;
		long carry3;
		long carry4;
		long carry5;
		long carry6;
		long carry7;
		long carry8;
		long carry9;

		// Remember: 2^255 congruent 19 modulo p
		carry9 = (h9 + (long) (1<<24)) >> 25; h0 += carry9 * 19; h9 -= carry9 << 25;
		carry1 = (h1 + (long) (1<<24)) >> 25; h2 += carry1; h1 -= carry1 << 25;
		carry3 = (h3 + (long) (1<<24)) >> 25; h4 += carry3; h3 -= carry3 << 25;
		carry5 = (h5 + (long) (1<<24)) >> 25; h6 += carry5; h5 -= carry5 << 25;
		carry7 = (h7 + (long) (1<<24)) >> 25; h8 += carry7; h7 -= carry7 << 25;

		carry0 = (h0 + (long) (1<<25)) >> 26; h1 += carry0; h0 -= carry0 << 26;
		carry2 = (h2 + (long) (1<<25)) >> 26; h3 += carry2; h2 -= carry2 << 26;
		carry4 = (h4 + (long) (1<<25)) >> 26; h5 += carry4; h4 -= carry4 << 26;
		carry6 = (h6 + (long) (1<<25)) >> 26; h7 += carry6; h6 -= carry6 << 26;
		carry8 = (h8 + (long) (1<<25)) >> 26; h9 += carry8; h8 -= carry8 << 26;

		int[] h = new int[10];
		h[0] = (int) h0;
		h[1] = (int) h1;
		h[2] = (int) h2;
		h[3] = (int) h3;
		h[4] = (int) h4;
		h[5] = (int) h5;
		h[6] = (int) h6;
		h[7] = (int) h7;
		h[8] = (int) h8;
		h[9] = (int) h9;

		return new Ed25519FieldElement(h);
	}

	/**
	 * Reduces an integer in 64 byte 2^8 bit representation modulo the group order q.
	 *
	 * Input:
	 *   s[0] + 256 * s[1] + ... + 256^63 * s[63] = s
	 *
	 * Output:
	 *   s[0] + 256 * s[1]+ ... +256^31 * s[31] = s mod q
	 *   where q = 2^252 + 27742317777372353535851937790883648493.
	 *
	 *   @param s The given integer.
	 */
	public static byte[] modQ(final byte[] s) {
		// s0, ..., s22 have 21 bits, s23 has 29 bits
		long s0 = 0x1FFFFF & threeBytesToLong(s, 0);
		long s1 = 0x1FFFFF & (fourBytesToLong(s, 2) >> 5);
		long s2 = 0x1FFFFF & (threeBytesToLong(s, 5) >> 2);
		long s3 = 0x1FFFFF & (fourBytesToLong(s, 7) >> 7);
		long s4 = 0x1FFFFF & (fourBytesToLong(s, 10) >> 4);
		long s5 = 0x1FFFFF & (threeBytesToLong(s, 13) >> 1);
		long s6 = 0x1FFFFF & (fourBytesToLong(s, 15) >> 6);
		long s7 = 0x1FFFFF & (threeBytesToLong(s, 18) >> 3);
		long s8 = 0x1FFFFF & threeBytesToLong(s, 21);
		long s9 = 0x1FFFFF & (fourBytesToLong(s, 23) >> 5);
		long s10 = 0x1FFFFF & (threeBytesToLong(s, 26) >> 2);
		long s11 = 0x1FFFFF & (fourBytesToLong(s, 28) >> 7);
		long s12 = 0x1FFFFF & (fourBytesToLong(s, 31) >> 4);
		long s13 = 0x1FFFFF & (threeBytesToLong(s, 34) >> 1);
		long s14 = 0x1FFFFF & (fourBytesToLong(s, 36) >> 6);
		long s15 = 0x1FFFFF & (threeBytesToLong(s, 39) >> 3);
		long s16 = 0x1FFFFF & threeBytesToLong(s, 42);
		long s17 = 0x1FFFFF & (fourBytesToLong(s, 44) >> 5);
		long s18 = 0x1FFFFF & (threeBytesToLong(s, 47) >> 2);
		long s19 = 0x1FFFFF & (fourBytesToLong(s, 49) >> 7);
		long s20 = 0x1FFFFF & (fourBytesToLong(s, 52) >> 4);
		long s21 = 0x1FFFFF & (threeBytesToLong(s, 55) >> 1);
		long s22 = 0x1FFFFF & (fourBytesToLong(s, 57) >> 6);
		long s23 = (fourBytesToLong(s, 60) >> 3);
		long carry0;
		long carry1;
		long carry2;
		long carry3;
		long carry4;
		long carry5;
		long carry6;
		long carry7;
		long carry8;
		long carry9;
		long carry10;
		long carry11;
		long carry12;
		long carry13;
		long carry14;
		long carry15;
		long carry16;

		/**
		 * Lots of magic numbers :)
		 * To understand what's going on below, note that
		 *
		 * (1) q = 2^252 + q0 where q0 = 27742317777372353535851937790883648493.
		 * (2) s11 is the coefficient of 2^(11*21), s23 is the coefficient of 2^(^23*21) and 2^252 = 2^((23-11) * 21)).
		 * (3) 2^252 congruent -q0 modulo q.
		 * (4) -q0 = 666643 * 2^0 + 470296 * 2^21 + 654183 * 2^(2*21) - 997805 * 2^(3*21) + 136657 * 2^(4*21) - 683901 * 2^(5*21)
		 *
		 * Thus
		 * s23 * 2^(23*11) = s23 * 2^(12*21) * 2^(11*21) = s3 * 2^252 * 2^(11*21) congruent
		 * s23 * (666643 * 2^0 + 470296 * 2^21 + 654183 * 2^(2*21) - 997805 * 2^(3*21) + 136657 * 2^(4*21) - 683901 * 2^(5*21)) * 2^(11*21) modulo q =
		 * s23 * (666643 * 2^(11*21) + 470296 * 2^(12*21) + 654183 * 2^(13*21) - 997805 * 2^(14*21) + 136657 * 2^(15*21) - 683901 * 2^(16*21)).
		 *
		 * The same procedure is then applied for s22,...,s18.
		 */
		s11 += s23 * 666643;
		s12 += s23 * 470296;
		s13 += s23 * 654183;
		s14 -= s23 * 997805;
		s15 += s23 * 136657;
		s16 -= s23 * 683901;

		s10 += s22 * 666643;
		s11 += s22 * 470296;
		s12 += s22 * 654183;
		s13 -= s22 * 997805;
		s14 += s22 * 136657;
		s15 -= s22 * 683901;

		s9 += s21 * 666643;
		s10 += s21 * 470296;
		s11 += s21 * 654183;
		s12 -= s21 * 997805;
		s13 += s21 * 136657;
		s14 -= s21 * 683901;

		s8 += s20 * 666643;
		s9 += s20 * 470296;
		s10 += s20 * 654183;
		s11 -= s20 * 997805;
		s12 += s20 * 136657;
		s13 -= s20 * 683901;

		s7 += s19 * 666643;
		s8 += s19 * 470296;
		s9 += s19 * 654183;
		s10 -= s19 * 997805;
		s11 += s19 * 136657;
		s12 -= s19 * 683901;

		s6 += s18 * 666643;
		s7 += s18 * 470296;
		s8 += s18 * 654183;
		s9 -= s18 * 997805;
		s10 += s18 * 136657;
		s11 -= s18 * 683901;

		/**
		 * Time to reduce the coefficient in order not to get an overflow.
		 */
		carry6 = (s6 + (1<<20)) >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry8 = (s8 + (1<<20)) >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry10 = (s10 + (1<<20)) >> 21; s11 += carry10; s10 -= carry10 << 21;
		carry12 = (s12 + (1<<20)) >> 21; s13 += carry12; s12 -= carry12 << 21;
		carry14 = (s14 + (1<<20)) >> 21; s15 += carry14; s14 -= carry14 << 21;
		carry16 = (s16 + (1<<20)) >> 21; s17 += carry16; s16 -= carry16 << 21;

		carry7 = (s7 + (1<<20)) >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry9 = (s9 + (1<<20)) >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry11 = (s11 + (1<<20)) >> 21; s12 += carry11; s11 -= carry11 << 21;
		carry13 = (s13 + (1<<20)) >> 21; s14 += carry13; s13 -= carry13 << 21;
		carry15 = (s15 + (1<<20)) >> 21; s16 += carry15; s15 -= carry15 << 21;

		/**
		 * Continue with above procedure.
		 */
		s5 += s17 * 666643;
		s6 += s17 * 470296;
		s7 += s17 * 654183;
		s8 -= s17 * 997805;
		s9 += s17 * 136657;
		s10 -= s17 * 683901;

		s4 += s16 * 666643;
		s5 += s16 * 470296;
		s6 += s16 * 654183;
		s7 -= s16 * 997805;
		s8 += s16 * 136657;
		s9 -= s16 * 683901;

		s3 += s15 * 666643;
		s4 += s15 * 470296;
		s5 += s15 * 654183;
		s6 -= s15 * 997805;
		s7 += s15 * 136657;
		s8 -= s15 * 683901;

		s2 += s14 * 666643;
		s3 += s14 * 470296;
		s4 += s14 * 654183;
		s5 -= s14 * 997805;
		s6 += s14 * 136657;
		s7 -= s14 * 683901;

		s1 += s13 * 666643;
		s2 += s13 * 470296;
		s3 += s13 * 654183;
		s4 -= s13 * 997805;
		s5 += s13 * 136657;
		s6 -= s13 * 683901;

		s0 += s12 * 666643;
		s1 += s12 * 470296;
		s2 += s12 * 654183;
		s3 -= s12 * 997805;
		s4 += s12 * 136657;
		s5 -= s12 * 683901;

		/**
		 * Reduce coefficients again.
		 */
		carry0 = (s0 + (1<<20)) >> 21; s1 += carry0; s0 -= carry0 << 21;
		carry2 = (s2 + (1<<20)) >> 21; s3 += carry2; s2 -= carry2 << 21;
		carry4 = (s4 + (1<<20)) >> 21; s5 += carry4; s4 -= carry4 << 21;
		carry6 = (s6 + (1<<20)) >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry8 = (s8 + (1<<20)) >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry10 = (s10 + (1<<20)) >> 21; s11 += carry10; s10 -= carry10 << 21;

		carry1 = (s1 + (1<<20)) >> 21; s2 += carry1; s1 -= carry1 << 21;
		carry3 = (s3 + (1<<20)) >> 21; s4 += carry3; s3 -= carry3 << 21;
		carry5 = (s5 + (1<<20)) >> 21; s6 += carry5; s5 -= carry5 << 21;
		carry7 = (s7 + (1<<20)) >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry9 = (s9 + (1<<20)) >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry11 = (s11 + (1<<20)) >> 21; s12 += carry11; s11 -= carry11 << 21;

		s0 += s12 * 666643;
		s1 += s12 * 470296;
		s2 += s12 * 654183;
		s3 -= s12 * 997805;
		s4 += s12 * 136657;
		s5 -= s12 * 683901;
		s12 = 0;

		carry0 = s0 >> 21; s1 += carry0; s0 -= carry0 << 21;
		carry1 = s1 >> 21; s2 += carry1; s1 -= carry1 << 21;
		carry2 = s2 >> 21; s3 += carry2; s2 -= carry2 << 21;
		carry3 = s3 >> 21; s4 += carry3; s3 -= carry3 << 21;
		carry4 = s4 >> 21; s5 += carry4; s4 -= carry4 << 21;
		carry5 = s5 >> 21; s6 += carry5; s5 -= carry5 << 21;
		carry6 = s6 >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry7 = s7 >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry8 = s8 >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry9 = s9 >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry10 = s10 >> 21; s11 += carry10; s10 -= carry10 << 21;
		carry11 = s11 >> 21; s12 += carry11; s11 -= carry11 << 21;

		// TODO-CR BR: Is it really needed to do it TWO times? (it doesn't hurt, just a question).
		s0 += s12 * 666643;
		s1 += s12 * 470296;
		s2 += s12 * 654183;
		s3 -= s12 * 997805;
		s4 += s12 * 136657;
		s5 -= s12 * 683901;

		carry0 = s0 >> 21; s1 += carry0; s0 -= carry0 << 21;
		carry1 = s1 >> 21; s2 += carry1; s1 -= carry1 << 21;
		carry2 = s2 >> 21; s3 += carry2; s2 -= carry2 << 21;
		carry3 = s3 >> 21; s4 += carry3; s3 -= carry3 << 21;
		carry4 = s4 >> 21; s5 += carry4; s4 -= carry4 << 21;
		carry5 = s5 >> 21; s6 += carry5; s5 -= carry5 << 21;
		carry6 = s6 >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry7 = s7 >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry8 = s8 >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry9 = s9 >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry10 = s10 >> 21; s11 += carry10; s10 -= carry10 << 21;

		// s0, ..., s11 got 21 bits each.
		byte[] result = new byte[32];
		result[0] = (byte) (s0 >> 0);
		result[1] = (byte) (s0 >> 8);
		result[2] = (byte) ((s0 >> 16) | (s1 << 5));
		result[3] = (byte) (s1 >> 3);
		result[4] = (byte) (s1 >> 11);
		result[5] = (byte) ((s1 >> 19) | (s2 << 2));
		result[6] = (byte) (s2 >> 6);
		result[7] = (byte) ((s2 >> 14) | (s3 << 7));
		result[8] = (byte) (s3 >> 1);
		result[9] = (byte) (s3 >> 9);
		result[10] = (byte) ((s3 >> 17) | (s4 << 4));
		result[11] = (byte) (s4 >> 4);
		result[12] = (byte) (s4 >> 12);
		result[13] = (byte) ((s4 >> 20) | (s5 << 1));
		result[14] = (byte) (s5 >> 7);
		result[15] = (byte) ((s5 >> 15) | (s6 << 6));
		result[16] = (byte) (s6 >> 2);
		result[17] = (byte) (s6 >> 10);
		result[18] = (byte) ((s6 >> 18) | (s7 << 3));
		result[19] = (byte) (s7 >> 5);
		result[20] = (byte) (s7 >> 13);
		result[21] = (byte) (s8 >> 0);
		result[22] = (byte) (s8 >> 8);
		result[23] = (byte) ((s8 >> 16) | (s9 << 5));
		result[24] = (byte) (s9 >> 3);
		result[25] = (byte) (s9 >> 11);
		result[26] = (byte) ((s9 >> 19) | (s10 << 2));
		result[27] = (byte) (s10 >> 6);
		result[28] = (byte) ((s10 >> 14) | (s11 << 7));
		result[29] = (byte) (s11 >> 1);
		result[30] = (byte) (s11 >> 9);
		result[31] = (byte) (s11 >> 17);

		return result;
	}

	/**
	 * Multiplies two encoded field elements and add a third. The result is reduced modulo the group order.
	 *
	 * Input:
	 *   a[0] + 256*a[1] + ... + 256^31 * a[31] = a
	 *   b[0] + 256*b[1] + ... + 256^31 * b[31] = b
	 *   c[0] + 256*c[1] + ... + 256^31 * c[31] = c
	 *
	 * Output:
	 *   result[0] + 256 * result[1] + ... + 256^31 * result[31] = (ab+c) mod q
	 *   where q = 2^252 + 27742317777372353535851937790883648493.
	 *
	 * See the comments in the method modQ() for an explanation of the algorithm.
	 *
	 * @param a The first integer.
	 * @param b The second integer.
	 * @param c The third integer.
	 */
	public static byte[] multiplyAndAddModQ(final byte[] a, final byte[] b, final byte[] c) {
		long a0 = 0x1FFFFF & threeBytesToLong(a, 0);
		long a1 = 0x1FFFFF & (fourBytesToLong(a, 2) >> 5);
		long a2 = 0x1FFFFF & (threeBytesToLong(a, 5) >> 2);
		long a3 = 0x1FFFFF & (fourBytesToLong(a, 7) >> 7);
		long a4 = 0x1FFFFF & (fourBytesToLong(a, 10) >> 4);
		long a5 = 0x1FFFFF & (threeBytesToLong(a, 13) >> 1);
		long a6 = 0x1FFFFF & (fourBytesToLong(a, 15) >> 6);
		long a7 = 0x1FFFFF & (threeBytesToLong(a, 18) >> 3);
		long a8 = 0x1FFFFF & threeBytesToLong(a, 21);
		long a9 = 0x1FFFFF & (fourBytesToLong(a, 23) >> 5);
		long a10 = 0x1FFFFF & (threeBytesToLong(a, 26) >> 2);
		long a11 = (fourBytesToLong(a, 28) >> 7);
		long b0 = 0x1FFFFF & threeBytesToLong(b, 0);
		long b1 = 0x1FFFFF & (fourBytesToLong(b, 2) >> 5);
		long b2 = 0x1FFFFF & (threeBytesToLong(b, 5) >> 2);
		long b3 = 0x1FFFFF & (fourBytesToLong(b, 7) >> 7);
		long b4 = 0x1FFFFF & (fourBytesToLong(b, 10) >> 4);
		long b5 = 0x1FFFFF & (threeBytesToLong(b, 13) >> 1);
		long b6 = 0x1FFFFF & (fourBytesToLong(b, 15) >> 6);
		long b7 = 0x1FFFFF & (threeBytesToLong(b, 18) >> 3);
		long b8 = 0x1FFFFF & threeBytesToLong(b, 21);
		long b9 = 0x1FFFFF & (fourBytesToLong(b, 23) >> 5);
		long b10 = 0x1FFFFF & (threeBytesToLong(b, 26) >> 2);
		long b11 = (fourBytesToLong(b, 28) >> 7);
		long c0 = 0x1FFFFF & threeBytesToLong(c, 0);
		long c1 = 0x1FFFFF & (fourBytesToLong(c, 2) >> 5);
		long c2 = 0x1FFFFF & (threeBytesToLong(c, 5) >> 2);
		long c3 = 0x1FFFFF & (fourBytesToLong(c, 7) >> 7);
		long c4 = 0x1FFFFF & (fourBytesToLong(c, 10) >> 4);
		long c5 = 0x1FFFFF & (threeBytesToLong(c, 13) >> 1);
		long c6 = 0x1FFFFF & (fourBytesToLong(c, 15) >> 6);
		long c7 = 0x1FFFFF & (threeBytesToLong(c, 18) >> 3);
		long c8 = 0x1FFFFF & threeBytesToLong(c, 21);
		long c9 = 0x1FFFFF & (fourBytesToLong(c, 23) >> 5);
		long c10 = 0x1FFFFF & (threeBytesToLong(c, 26) >> 2);
		long c11 = (fourBytesToLong(c, 28) >> 7);
		long s0;
		long s1;
		long s2;
		long s3;
		long s4;
		long s5;
		long s6;
		long s7;
		long s8;
		long s9;
		long s10;
		long s11;
		long s12;
		long s13;
		long s14;
		long s15;
		long s16;
		long s17;
		long s18;
		long s19;
		long s20;
		long s21;
		long s22;
		long s23;
		long carry0;
		long carry1;
		long carry2;
		long carry3;
		long carry4;
		long carry5;
		long carry6;
		long carry7;
		long carry8;
		long carry9;
		long carry10;
		long carry11;
		long carry12;
		long carry13;
		long carry14;
		long carry15;
		long carry16;
		long carry17;
		long carry18;
		long carry19;
		long carry20;
		long carry21;
		long carry22;

		s0 = c0 + a0*b0;
		s1 = c1 + a0*b1 + a1*b0;
		s2 = c2 + a0*b2 + a1*b1 + a2*b0;
		s3 = c3 + a0*b3 + a1*b2 + a2*b1 + a3*b0;
		s4 = c4 + a0*b4 + a1*b3 + a2*b2 + a3*b1 + a4*b0;
		s5 = c5 + a0*b5 + a1*b4 + a2*b3 + a3*b2 + a4*b1 + a5*b0;
		s6 = c6 + a0*b6 + a1*b5 + a2*b4 + a3*b3 + a4*b2 + a5*b1 + a6*b0;
		s7 = c7 + a0*b7 + a1*b6 + a2*b5 + a3*b4 + a4*b3 + a5*b2 + a6*b1 + a7*b0;
		s8 = c8 + a0*b8 + a1*b7 + a2*b6 + a3*b5 + a4*b4 + a5*b3 + a6*b2 + a7*b1 + a8*b0;
		s9 = c9 + a0*b9 + a1*b8 + a2*b7 + a3*b6 + a4*b5 + a5*b4 + a6*b3 + a7*b2 + a8*b1 + a9*b0;
		s10 = c10 + a0*b10 + a1*b9 + a2*b8 + a3*b7 + a4*b6 + a5*b5 + a6*b4 + a7*b3 + a8*b2 + a9*b1 + a10*b0;
		s11 = c11 + a0*b11 + a1*b10 + a2*b9 + a3*b8 + a4*b7 + a5*b6 + a6*b5 + a7*b4 + a8*b3 + a9*b2 + a10*b1 + a11*b0;
		s12 = a1*b11 + a2*b10 + a3*b9 + a4*b8 + a5*b7 + a6*b6 + a7*b5 + a8*b4 + a9*b3 + a10*b2 + a11*b1;
		s13 = a2*b11 + a3*b10 + a4*b9 + a5*b8 + a6*b7 + a7*b6 + a8*b5 + a9*b4 + a10*b3 + a11*b2;
		s14 = a3*b11 + a4*b10 + a5*b9 + a6*b8 + a7*b7 + a8*b6 + a9*b5 + a10*b4 + a11*b3;
		s15 = a4*b11 + a5*b10 + a6*b9 + a7*b8 + a8*b7 + a9*b6 + a10*b5 + a11*b4;
		s16 = a5*b11 + a6*b10 + a7*b9 + a8*b8 + a9*b7 + a10*b6 + a11*b5;
		s17 = a6*b11 + a7*b10 + a8*b9 + a9*b8 + a10*b7 + a11*b6;
		s18 = a7*b11 + a8*b10 + a9*b9 + a10*b8 + a11*b7;
		s19 = a8*b11 + a9*b10 + a10*b9 + a11*b8;
		s20 = a9*b11 + a10*b10 + a11*b9;
		s21 = a10*b11 + a11*b10;
		s22 = a11*b11;
		s23 = 0;

		carry0 = (s0 + (1<<20)) >> 21; s1 += carry0; s0 -= carry0 << 21;
		carry2 = (s2 + (1<<20)) >> 21; s3 += carry2; s2 -= carry2 << 21;
		carry4 = (s4 + (1<<20)) >> 21; s5 += carry4; s4 -= carry4 << 21;
		carry6 = (s6 + (1<<20)) >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry8 = (s8 + (1<<20)) >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry10 = (s10 + (1<<20)) >> 21; s11 += carry10; s10 -= carry10 << 21;
		carry12 = (s12 + (1<<20)) >> 21; s13 += carry12; s12 -= carry12 << 21;
		carry14 = (s14 + (1<<20)) >> 21; s15 += carry14; s14 -= carry14 << 21;
		carry16 = (s16 + (1<<20)) >> 21; s17 += carry16; s16 -= carry16 << 21;
		carry18 = (s18 + (1<<20)) >> 21; s19 += carry18; s18 -= carry18 << 21;
		carry20 = (s20 + (1<<20)) >> 21; s21 += carry20; s20 -= carry20 << 21;
		carry22 = (s22 + (1<<20)) >> 21; s23 += carry22; s22 -= carry22 << 21;

		carry1 = (s1 + (1<<20)) >> 21; s2 += carry1; s1 -= carry1 << 21;
		carry3 = (s3 + (1<<20)) >> 21; s4 += carry3; s3 -= carry3 << 21;
		carry5 = (s5 + (1<<20)) >> 21; s6 += carry5; s5 -= carry5 << 21;
		carry7 = (s7 + (1<<20)) >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry9 = (s9 + (1<<20)) >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry11 = (s11 + (1<<20)) >> 21; s12 += carry11; s11 -= carry11 << 21;
		carry13 = (s13 + (1<<20)) >> 21; s14 += carry13; s13 -= carry13 << 21;
		carry15 = (s15 + (1<<20)) >> 21; s16 += carry15; s15 -= carry15 << 21;
		carry17 = (s17 + (1<<20)) >> 21; s18 += carry17; s17 -= carry17 << 21;
		carry19 = (s19 + (1<<20)) >> 21; s20 += carry19; s19 -= carry19 << 21;
		carry21 = (s21 + (1<<20)) >> 21; s22 += carry21; s21 -= carry21 << 21;

		s11 += s23 * 666643;
		s12 += s23 * 470296;
		s13 += s23 * 654183;
		s14 -= s23 * 997805;
		s15 += s23 * 136657;
		s16 -= s23 * 683901;

		s10 += s22 * 666643;
		s11 += s22 * 470296;
		s12 += s22 * 654183;
		s13 -= s22 * 997805;
		s14 += s22 * 136657;
		s15 -= s22 * 683901;

		s9 += s21 * 666643;
		s10 += s21 * 470296;
		s11 += s21 * 654183;
		s12 -= s21 * 997805;
		s13 += s21 * 136657;
		s14 -= s21 * 683901;

		s8 += s20 * 666643;
		s9 += s20 * 470296;
		s10 += s20 * 654183;
		s11 -= s20 * 997805;
		s12 += s20 * 136657;
		s13 -= s20 * 683901;

		s7 += s19 * 666643;
		s8 += s19 * 470296;
		s9 += s19 * 654183;
		s10 -= s19 * 997805;
		s11 += s19 * 136657;
		s12 -= s19 * 683901;

		s6 += s18 * 666643;
		s7 += s18 * 470296;
		s8 += s18 * 654183;
		s9 -= s18 * 997805;
		s10 += s18 * 136657;
		s11 -= s18 * 683901;

		carry6 = (s6 + (1<<20)) >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry8 = (s8 + (1<<20)) >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry10 = (s10 + (1<<20)) >> 21; s11 += carry10; s10 -= carry10 << 21;
		carry12 = (s12 + (1<<20)) >> 21; s13 += carry12; s12 -= carry12 << 21;
		carry14 = (s14 + (1<<20)) >> 21; s15 += carry14; s14 -= carry14 << 21;
		carry16 = (s16 + (1<<20)) >> 21; s17 += carry16; s16 -= carry16 << 21;

		carry7 = (s7 + (1<<20)) >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry9 = (s9 + (1<<20)) >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry11 = (s11 + (1<<20)) >> 21; s12 += carry11; s11 -= carry11 << 21;
		carry13 = (s13 + (1<<20)) >> 21; s14 += carry13; s13 -= carry13 << 21;
		carry15 = (s15 + (1<<20)) >> 21; s16 += carry15; s15 -= carry15 << 21;

		s5 += s17 * 666643;
		s6 += s17 * 470296;
		s7 += s17 * 654183;
		s8 -= s17 * 997805;
		s9 += s17 * 136657;
		s10 -= s17 * 683901;

		s4 += s16 * 666643;
		s5 += s16 * 470296;
		s6 += s16 * 654183;
		s7 -= s16 * 997805;
		s8 += s16 * 136657;
		s9 -= s16 * 683901;

		s3 += s15 * 666643;
		s4 += s15 * 470296;
		s5 += s15 * 654183;
		s6 -= s15 * 997805;
		s7 += s15 * 136657;
		s8 -= s15 * 683901;

		s2 += s14 * 666643;
		s3 += s14 * 470296;
		s4 += s14 * 654183;
		s5 -= s14 * 997805;
		s6 += s14 * 136657;
		s7 -= s14 * 683901;

		s1 += s13 * 666643;
		s2 += s13 * 470296;
		s3 += s13 * 654183;
		s4 -= s13 * 997805;
		s5 += s13 * 136657;
		s6 -= s13 * 683901;

		s0 += s12 * 666643;
		s1 += s12 * 470296;
		s2 += s12 * 654183;
		s3 -= s12 * 997805;
		s4 += s12 * 136657;
		s5 -= s12 * 683901;
		s12 = 0;

		carry0 = (s0 + (1<<20)) >> 21; s1 += carry0; s0 -= carry0 << 21;
		carry2 = (s2 + (1<<20)) >> 21; s3 += carry2; s2 -= carry2 << 21;
		carry4 = (s4 + (1<<20)) >> 21; s5 += carry4; s4 -= carry4 << 21;
		carry6 = (s6 + (1<<20)) >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry8 = (s8 + (1<<20)) >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry10 = (s10 + (1<<20)) >> 21; s11 += carry10; s10 -= carry10 << 21;

		carry1 = (s1 + (1<<20)) >> 21; s2 += carry1; s1 -= carry1 << 21;
		carry3 = (s3 + (1<<20)) >> 21; s4 += carry3; s3 -= carry3 << 21;
		carry5 = (s5 + (1<<20)) >> 21; s6 += carry5; s5 -= carry5 << 21;
		carry7 = (s7 + (1<<20)) >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry9 = (s9 + (1<<20)) >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry11 = (s11 + (1<<20)) >> 21; s12 += carry11; s11 -= carry11 << 21;

		s0 += s12 * 666643;
		s1 += s12 * 470296;
		s2 += s12 * 654183;
		s3 -= s12 * 997805;
		s4 += s12 * 136657;
		s5 -= s12 * 683901;
		s12 = 0;

		carry0 = s0 >> 21; s1 += carry0; s0 -= carry0 << 21;
		carry1 = s1 >> 21; s2 += carry1; s1 -= carry1 << 21;
		carry2 = s2 >> 21; s3 += carry2; s2 -= carry2 << 21;
		carry3 = s3 >> 21; s4 += carry3; s3 -= carry3 << 21;
		carry4 = s4 >> 21; s5 += carry4; s4 -= carry4 << 21;
		carry5 = s5 >> 21; s6 += carry5; s5 -= carry5 << 21;
		carry6 = s6 >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry7 = s7 >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry8 = s8 >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry9 = s9 >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry10 = s10 >> 21; s11 += carry10; s10 -= carry10 << 21;
		carry11 = s11 >> 21; s12 += carry11; s11 -= carry11 << 21;

		s0 += s12 * 666643;
		s1 += s12 * 470296;
		s2 += s12 * 654183;
		s3 -= s12 * 997805;
		s4 += s12 * 136657;
		s5 -= s12 * 683901;

		carry0 = s0 >> 21; s1 += carry0; s0 -= carry0 << 21;
		carry1 = s1 >> 21; s2 += carry1; s1 -= carry1 << 21;
		carry2 = s2 >> 21; s3 += carry2; s2 -= carry2 << 21;
		carry3 = s3 >> 21; s4 += carry3; s3 -= carry3 << 21;
		carry4 = s4 >> 21; s5 += carry4; s4 -= carry4 << 21;
		carry5 = s5 >> 21; s6 += carry5; s5 -= carry5 << 21;
		carry6 = s6 >> 21; s7 += carry6; s6 -= carry6 << 21;
		carry7 = s7 >> 21; s8 += carry7; s7 -= carry7 << 21;
		carry8 = s8 >> 21; s9 += carry8; s8 -= carry8 << 21;
		carry9 = s9 >> 21; s10 += carry9; s9 -= carry9 << 21;
		carry10 = s10 >> 21; s11 += carry10; s10 -= carry10 << 21;

		byte[] result = new byte[32];
		result[0] = (byte) (s0 >> 0);
		result[1] = (byte) (s0 >> 8);
		result[2] = (byte) ((s0 >> 16) | (s1 << 5));
		result[3] = (byte) (s1 >> 3);
		result[4] = (byte) (s1 >> 11);
		result[5] = (byte) ((s1 >> 19) | (s2 << 2));
		result[6] = (byte) (s2 >> 6);
		result[7] = (byte) ((s2 >> 14) | (s3 << 7));
		result[8] = (byte) (s3 >> 1);
		result[9] = (byte) (s3 >> 9);
		result[10] = (byte) ((s3 >> 17) | (s4 << 4));
		result[11] = (byte) (s4 >> 4);
		result[12] = (byte) (s4 >> 12);
		result[13] = (byte) ((s4 >> 20) | (s5 << 1));
		result[14] = (byte) (s5 >> 7);
		result[15] = (byte) ((s5 >> 15) | (s6 << 6));
		result[16] = (byte) (s6 >> 2);
		result[17] = (byte) (s6 >> 10);
		result[18] = (byte) ((s6 >> 18) | (s7 << 3));
		result[19] = (byte) (s7 >> 5);
		result[20] = (byte) (s7 >> 13);
		result[21] = (byte) (s8 >> 0);
		result[22] = (byte) (s8 >> 8);
		result[23] = (byte) ((s8 >> 16) | (s9 << 5));
		result[24] = (byte) (s9 >> 3);
		result[25] = (byte) (s9 >> 11);
		result[26] = (byte) ((s9 >> 19) | (s10 << 2));
		result[27] = (byte) (s10 >> 6);
		result[28] = (byte) ((s10 >> 14) | (s11 << 7));
		result[29] = (byte) (s11 >> 1);
		result[30] = (byte) (s11 >> 9);
		result[31] = (byte) (s11 >> 17);

		return result;
	}

	private static long threeBytesToLong(final byte[] in, int offset) {
		int result = in[offset++] & 0xff;
		result |= (in[offset++] & 0xff) << 8;
		result |= (in[offset] & 0xff) << 16;
		return result;
	}

	private static long fourBytesToLong(final byte[] in, int offset) {
		int result = in[offset++] & 0xff;
		result |= (in[offset++] & 0xff) << 8;
		result |= (in[offset++] & 0xff) << 16;
		result |= in[offset] << 24;
		return ((long)result) & 0xffffffffL;
	}

	/**
	 * Return true if this is in {1,3,5,...,q-2}
	 * Return false if this is in {0,2,4,...,q-1}
	 *
	 * Preconditions:
	 *    |x| bounded by 1.1*2^26,1.1*2^25,1.1*2^26,1.1*2^25,etc.
	 *
	 * @return true if this is in {1,3,5,...,q-2}, false otherwise.
	 */
	public boolean isNegative() {
		byte[] s = encode();
		return (s[0] & 1) != 0;
	}

	@Override
    public int hashCode() {
        return Arrays.hashCode(this.values);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Ed25519FieldElement)) {
			return false;
		}

		final Ed25519FieldElement f = (Ed25519FieldElement) obj;
        return 1 == ArrayUtils.isEqual(this.encode(), f.encode());
    }

    @Override
    public String toString() {
        return HexEncoder.getString(this.encode());
    }
}

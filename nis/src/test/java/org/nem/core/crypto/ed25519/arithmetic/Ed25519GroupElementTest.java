package org.nem.core.crypto.ed25519.arithmetic;

import org.hamcrest.core.*;
import org.junit.*;

import java.math.BigInteger;
import java.util.Arrays;

public class Ed25519GroupElementTest {

    @Test
    public void canBeCreatedWithP2Coordinates() {
		final Ed25519GroupElement g = Ed25519GroupElement.p2(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE);
		Assert.assertThat(g.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P2));
		Assert.assertThat(g.getX(), IsEqual.equalTo(Ed25519Field.ZERO));
		Assert.assertThat(g.getY(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getZ(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getT(), IsEqual.equalTo(null));
    }

    @Test
	public void canBeCreatedWithP3Coordinates() {
		final Ed25519GroupElement g = Ed25519GroupElement.p3(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);
		Assert.assertThat(g.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P3));
		Assert.assertThat(g.getX(), IsEqual.equalTo(Ed25519Field.ZERO));
		Assert.assertThat(g.getY(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getZ(), IsEqual.equalTo(Ed25519Field.ONE));
	}

    @Test
	public void canBeCreatedWithP1P1Coordinates() {
		final Ed25519GroupElement g = Ed25519GroupElement.p1p1(Ed25519Field.ZERO, Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ONE);
		Assert.assertThat(g.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P1P1));
		Assert.assertThat(g.getX(), IsEqual.equalTo(Ed25519Field.ZERO));
		Assert.assertThat(g.getY(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getZ(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getT(), IsEqual.equalTo(Ed25519Field.ONE));
    }

    @Test
	public void canBeCreatedWithPrecompCoordinates() {
		final Ed25519GroupElement g = Ed25519GroupElement.precomp(Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);
		Assert.assertThat(g.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.PRECOMP));
		Assert.assertThat(g.getX(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getY(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getZ(), IsEqual.equalTo(Ed25519Field.ZERO));
		Assert.assertThat(g.getT(), IsEqual.equalTo(null));
    }

    @Test
	public void canBeCreatedWithCachedCoordinates() {
		final Ed25519GroupElement g = Ed25519GroupElement.cached(Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ONE, Ed25519Field.ZERO);
		Assert.assertThat(g.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.CACHED));
		Assert.assertThat(g.getX(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getY(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getZ(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getT(), IsEqual.equalTo(Ed25519Field.ZERO));
    }

    @Test
	public void canBeCreatedWithSpecifiedCoordinates() {
		final Ed25519GroupElement g = new Ed25519GroupElement(
				Ed25519GroupElement.Representation.P3,
				Ed25519Field.ZERO,
				Ed25519Field.ONE,
				Ed25519Field.ONE,
				Ed25519Field.ZERO);
		Assert.assertThat(g.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P3));
		Assert.assertThat(g.getX(), IsEqual.equalTo(Ed25519Field.ZERO));
		Assert.assertThat(g.getY(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getZ(), IsEqual.equalTo(Ed25519Field.ONE));
		Assert.assertThat(g.getT(), IsEqual.equalTo(Ed25519Field.ZERO));
    }

	@Test
	public void constructorUsingByteArrayReturnsExpectedResult() {
		for (int i=0; i<100; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();
			final byte[] bytes = g.toByteArray();

			// Act:
			final Ed25519GroupElement h1 = new Ed25519GroupElement(new Ed25519EncodedFieldElement(bytes));
			final Ed25519GroupElement h2 = MathUtils.toGroupElement(bytes);

			// Assert:
			Assert.assertThat(h1, IsEqual.equalTo(h2));
		}
	}

	 @Test
	 public void toByteArrayReturnsExpectedResult() {
		 for (int i=0; i<100; i++) {
			 // Arrange:
			 final Ed25519GroupElement g = MathUtils.getRandomGroupElement();

			 // Act:
			 final byte[] gBytes = g.toByteArray();
			 final byte[] bytes = MathUtils.toByteArray(MathUtils.toBigInteger(g.getY()));
			 if (MathUtils.toBigInteger(g.getX()).mod(new BigInteger("2")).equals(MathUtils.toBigInteger(Ed25519Field.ONE))) {
				 bytes[31] |= 0x80;
			 }

			 // Assert:
			 Assert.assertThat(Arrays.equals(gBytes, bytes), IsEqual.equalTo(true));
		 }
	 }

	@Test (expected = IllegalArgumentException.class)
	public void toP2ThrowsIfGroupElementHasPrecompRepresentation() {
		// Arrange:
		final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.PRECOMP);

		// Assert:
		g.toP2();
	}

	@Test (expected = IllegalArgumentException.class)
	public void toP2ThrowsIfGroupElementHasCachedRepresentation() {
		// Arrange:
		final Ed25519GroupElement g =  MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.CACHED);

		// Assert:
		g.toP2();
	}

	@Test
	public void toP2ReturnsExpectedResultIfGroupElementHasP2Representation() {
		for (int i=0; i<10; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.P2);

			// Act:
			final Ed25519GroupElement h = g.toP2();

			// Assert:
			Assert.assertThat(h, IsEqual.equalTo(g));
			Assert.assertThat(h.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P2));
			Assert.assertThat(h.getX(), IsEqual.equalTo(g.getX()));
			Assert.assertThat(h.getY(), IsEqual.equalTo(g.getY()));
			Assert.assertThat(h.getZ(), IsEqual.equalTo(g.getZ()));
			Assert.assertThat(h.getT(), IsEqual.equalTo(null));
		}
	}

	@Test
	public void toP2ReturnsExpectedResultIfGroupElementHasP3Representation() {
		for (int i=0; i<10; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();

			// Act:
			final Ed25519GroupElement h1 = g.toP2();
			final Ed25519GroupElement h2 = MathUtils.toRepresentation(g, Ed25519GroupElement.Representation.P2);

			// Assert:
			Assert.assertThat(h1, IsEqual.equalTo(h2));
			Assert.assertThat(h1.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P2));
			Assert.assertThat(h1.getX(), IsEqual.equalTo(g.getX()));
			Assert.assertThat(h1.getY(), IsEqual.equalTo(g.getY()));
			Assert.assertThat(h1.getZ(), IsEqual.equalTo(g.getZ()));
			Assert.assertThat(h1.getT(), IsEqual.equalTo(null));
		}
	}

	@Test
	public void toP2ReturnsExpectedResultIfGroupElementHasP1P1Representation() {
		for (int i=0; i<10; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.P1P1);

			// Act:
			final Ed25519GroupElement h1 = g.toP2();
			final Ed25519GroupElement h2 = MathUtils.toRepresentation(g, Ed25519GroupElement.Representation.P2);

			// Assert:
			Assert.assertThat(h1, IsEqual.equalTo(h2));
			Assert.assertThat(h1.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P2));
			Assert.assertThat(h1.getX(), IsEqual.equalTo(g.getX().multiply(g.getT())));
			Assert.assertThat(h1.getY(), IsEqual.equalTo(g.getY().multiply(g.getZ())));
			Assert.assertThat(h1.getZ(), IsEqual.equalTo(g.getZ().multiply(g.getT())));
			Assert.assertThat(h1.getT(), IsEqual.equalTo(null));
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void toP3ThrowsIfGroupElementHasP2Representation() {
		// Arrange:
		final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.P2);

		// Assert:
		g.toP3();
	}

	@Test (expected = IllegalArgumentException.class)
	public void toP3ThrowsIfGroupElementHasPrecompRepresentation() {
		// Arrange:
		final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.PRECOMP);

		// Assert:
		g.toP3();
	}

	@Test (expected = IllegalArgumentException.class)
	public void toP3ThrowsIfGroupElementHasCachedRepresentation() {
		// Arrange:
		final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.CACHED);

		// Assert:
		g.toP3();
	}

	@Test
	public void toP3ReturnsExpectedResultIfGroupElementHasP1P1Representation() {
		for (int i=0; i<10; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.P1P1);

			// Act:
			final Ed25519GroupElement h1 = g.toP3();
			final Ed25519GroupElement h2 = MathUtils.toRepresentation(g, Ed25519GroupElement.Representation.P3);

			// Assert:
			Assert.assertThat(h1, IsEqual.equalTo(h2));
			Assert.assertThat(h1.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P3));
			Assert.assertThat(h1.getX(), IsEqual.equalTo(g.getX().multiply(g.getT())));
			Assert.assertThat(h1.getY(), IsEqual.equalTo(g.getY().multiply(g.getZ())));
			Assert.assertThat(h1.getZ(), IsEqual.equalTo(g.getZ().multiply(g.getT())));
			Assert.assertThat(h1.getT(), IsEqual.equalTo(g.getX().multiply(g.getY())));
		}
	}

	@Test
	public void toP3ReturnsExpectedResultIfGroupElementHasP3Representation() {
		for (int i=0; i<10; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();

			// Act:
			final Ed25519GroupElement h = g.toP3();

			// Assert:
			Assert.assertThat(h, IsEqual.equalTo(g));
			Assert.assertThat(h.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.P3));
			Assert.assertThat(h, IsEqual.equalTo(g));
			Assert.assertThat(h.getX(), IsEqual.equalTo(g.getX()));
			Assert.assertThat(h.getY(), IsEqual.equalTo(g.getY()));
			Assert.assertThat(h.getZ(), IsEqual.equalTo(g.getZ()));
			Assert.assertThat(h.getT(), IsEqual.equalTo(g.getT()));
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void toCachedThrowsIfGroupElementHasP2Representation() {
		// Arrange:
		final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.P2);

		// Assert:
		g.toCached();
	}

	@Test (expected = IllegalArgumentException.class)
	public void toCachedThrowsIfGroupElementHasPrecompRepresentation() {
		// Arrange:
		final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.PRECOMP);

		// Assert:
		g.toCached();
	}

	@Test (expected = IllegalArgumentException.class)
	public void toCachedThrowsIfGroupElementHasP1P1Representation() {
		// Arrange:
		final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.P1P1);

		// Assert:
		g.toCached();
	}

	@Test
	public void toCachedReturnsExpectedResultIfGroupElementHasCachedRepresentation() {
		for (int i=0; i<10; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.toRepresentation(MathUtils.getRandomGroupElement(), Ed25519GroupElement.Representation.CACHED);

			// Act:
			final Ed25519GroupElement h = g.toCached();

			// Assert:
			Assert.assertThat(h, IsEqual.equalTo(g));
			Assert.assertThat(h.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.CACHED));
			Assert.assertThat(h, IsEqual.equalTo(g));
			Assert.assertThat(h.getX(), IsEqual.equalTo(g.getX()));
			Assert.assertThat(h.getY(), IsEqual.equalTo(g.getY()));
			Assert.assertThat(h.getZ(), IsEqual.equalTo(g.getZ()));
			Assert.assertThat(h.getT(), IsEqual.equalTo(g.getT()));
		}
	}

	@Test
	public void toCachedReturnsExpectedResultIfGroupElementHasP3Representation() {
		for (int i=0; i<10; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();

			// Act:
			final Ed25519GroupElement h1 = g.toCached();
			final Ed25519GroupElement h2 = MathUtils.toRepresentation(g, Ed25519GroupElement.Representation.CACHED);

			// Assert:
			Assert.assertThat(h1, IsEqual.equalTo(h2));
			Assert.assertThat(h1.getRepresentation(), IsEqual.equalTo(Ed25519GroupElement.Representation.CACHED));
			Assert.assertThat(h1, IsEqual.equalTo(g));
			Assert.assertThat(h1.getX(), IsEqual.equalTo(g.getY().add(g.getX())));
			Assert.assertThat(h1.getY(), IsEqual.equalTo(g.getY().subtract(g.getX())));
			Assert.assertThat(h1.getZ(), IsEqual.equalTo(g.getZ()));
			Assert.assertThat(h1.getT(), IsEqual.equalTo(g.getT().multiply(Ed25519Field.D_Times_TWO)));
		}
	}

	// endregion

	@Test
	public void precomputedTableContainsExpectedGroupElements() {
		// Arrange:
		Ed25519GroupElement g = Ed25519Group.BASE_POINT;

		// Act + Assert:
		for (int i = 0; i < 32; i++) {
			Ed25519GroupElement h = g;
			for (int j = 0; j < 8; j++) {
				Assert.assertThat(MathUtils.toRepresentation(h, Ed25519GroupElement.Representation.PRECOMP),
						IsEqual.equalTo(Ed25519Group.BASE_POINT.getPrecomputedForSingle()[i][j]));
				h = MathUtils.addGroupElements(h, g);
			}
			for (int k = 0; k < 8; k++) {
				g = MathUtils.addGroupElements(g, g);
			}
		}
	}

	@Test
	public void dblPrecomputedTableContainsExpectedGroupElements() {
		// Arrange:
		Ed25519GroupElement g = Ed25519Group.BASE_POINT;
		Ed25519GroupElement h = MathUtils.addGroupElements(g, g);

		// Act + Assert:
		for (int i=0; i<8; i++) {
			Assert.assertThat(MathUtils.toRepresentation(g, Ed25519GroupElement.Representation.PRECOMP),
					IsEqual.equalTo(Ed25519Group.BASE_POINT.getPrecomputedForDouble()[i]));
			g = MathUtils.addGroupElements(g, h);
		}
	}

	@Test
	public void dblReturnsExpectedResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();

			// Act:
			final Ed25519GroupElement h1 = g.dbl();
			final Ed25519GroupElement h2 = MathUtils.doubleGroupElement(g);

			// Assert:
			Assert.assertThat(h2, IsEqual.equalTo(h1));
		}
	}

	@Test
	public void addingNeutralGroupElementDoesNotChangeGroupElement() {
		final Ed25519GroupElement neutral = Ed25519GroupElement.p3(
				Ed25519Field.ZERO,
				Ed25519Field.ONE,
				Ed25519Field.ONE,
				Ed25519Field.ZERO);
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();

			// Act:
			final Ed25519GroupElement h1 = g.add(neutral.toCached());
			final Ed25519GroupElement h2 = neutral.add(g.toCached());

			// Assert:
			Assert.assertThat(g, IsEqual.equalTo(h1));
			Assert.assertThat(g, IsEqual.equalTo(h2));
		}
	}

	@Test
	public void addReturnsExpectedResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519GroupElement g1 = MathUtils.getRandomGroupElement();
			final Ed25519GroupElement g2 = MathUtils.getRandomGroupElement();

			// Act:
			final Ed25519GroupElement h1 = g1.add(g2.toCached());
			final Ed25519GroupElement h2 = MathUtils.addGroupElements(g1, g2);

			// Assert:
			Assert.assertThat(h2, IsEqual.equalTo(h1));
		}
	}

	@Test
	public void subReturnsExpectedResult() {
		for (int i=0; i<1000; i++) {
			// Arrange:
			final Ed25519GroupElement g1 = MathUtils.getRandomGroupElement();
			final Ed25519GroupElement g2 = MathUtils.getRandomGroupElement();

			// Act:
			final Ed25519GroupElement h1 = g1.sub(g2.toCached());
			final Ed25519GroupElement h2 = MathUtils.addGroupElements(g1, MathUtils.negateGroupElement(g2));

			// Assert:
			Assert.assertThat(h2, IsEqual.equalTo(h1));
		}
	}

	// region hashCode / equals

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Ed25519GroupElement g1 = MathUtils.getRandomGroupElement();
		final Ed25519GroupElement g2 = MathUtils.toRepresentation(g1, Ed25519GroupElement.Representation.P2);
		final Ed25519GroupElement g3 = MathUtils.toRepresentation(g1, Ed25519GroupElement.Representation.CACHED);
		final Ed25519GroupElement g4 = MathUtils.toRepresentation(g1, Ed25519GroupElement.Representation.P1P1);
		final Ed25519GroupElement g5 = MathUtils.getRandomGroupElement();

		// Assert
		Assert.assertThat(g2, IsEqual.equalTo(g1));
		Assert.assertThat(g3, IsEqual.equalTo(g1));
		Assert.assertThat(g1, IsEqual.equalTo(g4));
		Assert.assertThat(g1, IsNot.not(IsEqual.equalTo(g5)));
		Assert.assertThat(g2, IsNot.not(IsEqual.equalTo(g5)));
		Assert.assertThat(g3, IsNot.not(IsEqual.equalTo(g5)));
		Assert.assertThat(g5, IsNot.not(IsEqual.equalTo(g4)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Ed25519GroupElement g1 = MathUtils.getRandomGroupElement();
		final Ed25519GroupElement g2 = MathUtils.toRepresentation(g1, Ed25519GroupElement.Representation.P2);
		final Ed25519GroupElement g3 = MathUtils.toRepresentation(g1, Ed25519GroupElement.Representation.P1P1);
		final Ed25519GroupElement g4 = MathUtils.getRandomGroupElement();

		// Assert
		Assert.assertThat(g2.hashCode(), IsEqual.equalTo(g1.hashCode()));
		Assert.assertThat(g3.hashCode(), IsEqual.equalTo(g1.hashCode()));
		Assert.assertThat(g1.hashCode(), IsNot.not(IsEqual.equalTo(g4.hashCode())));
		Assert.assertThat(g2.hashCode(), IsNot.not(IsEqual.equalTo(g4.hashCode())));
		Assert.assertThat(g3.hashCode(), IsNot.not(IsEqual.equalTo(g4.hashCode())));
	}

	// endregion

	@Test
	public void scalarMultiplyBasePointWithZeroReturnsNeutralElement() {
		// Arrange:
		final Ed25519GroupElement basePoint = Ed25519Group.BASE_POINT;

		// Act:
		final Ed25519GroupElement g = basePoint.scalarMultiply(Ed25519Field.ZERO.encode());

		// Assert:
		Assert.assertThat(Ed25519Group.ZERO_P3, IsEqual.equalTo(g));
	}

	@Test
	public void scalarMultiplyBasePointWithOneReturnsBasePoint() {
		// Arrange:
		final Ed25519GroupElement basePoint = Ed25519Group.BASE_POINT;

		// Act:
		final Ed25519GroupElement g = basePoint.scalarMultiply(Ed25519Field.ONE.encode());

		// Assert:
		Assert.assertThat(basePoint, IsEqual.equalTo(g));
	}

	// This test is slow (~6s) due to math utils using an inferior algorithm to calculate the result.
	@Test
	public void scalarMultiplyBasePointReturnsExpectedResult() {
		for (int i=0; i<100; i++) {
			// Arrange:
			final Ed25519GroupElement basePoint = Ed25519Group.BASE_POINT;
			final Ed25519FieldElement f = MathUtils.getRandomFieldElement();

			// Act:
			final Ed25519GroupElement g = basePoint.scalarMultiply(f.encode());
			final Ed25519GroupElement h = MathUtils.scalarMultiplyGroupElement(basePoint, f);

			// Assert:
			Assert.assertThat(g, IsEqual.equalTo(h));
		}
	}

	// This test is slow (~6s) due to math utils using an inferior algorithm to calculate the result.
	@Test
	public void doubleScalarMultiplyVariableTimeReturnsExpectedResult() {
		for (int i=0; i<50; i++) {
			// Arrange:
			final Ed25519GroupElement basePoint = Ed25519Group.BASE_POINT;
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();
			g.precomputeForDoubleScalarMultiplication();
			final Ed25519FieldElement f1 = MathUtils.getRandomFieldElement();
			final Ed25519FieldElement f2 = MathUtils.getRandomFieldElement();

			// Act:
			final Ed25519GroupElement h1 = basePoint.doubleScalarMultiplyVariableTime(g, f2.encode(), f1.encode());
			final Ed25519GroupElement h2 = MathUtils.doubleScalarMultiplyGroupElements(basePoint, f1, g, f2);

			// Assert:
			Assert.assertThat(h1, IsEqual.equalTo(h2));
		}
	}

	// endregion

	@Test
	public void isOnCurveReturnsTrueForPointsOnTheCurve() {
		for (int i=0; i<100; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();

			// Assert:
			Assert.assertThat(g.isOnCurve(), IsEqual.equalTo(true));
		}
	}

	@Test
	public void isOnCurveReturnsFalseForPointsNotOnTheCurve() {
		for (int i=0; i<100; i++) {
			// Arrange:
			final Ed25519GroupElement g = MathUtils.getRandomGroupElement();
			final Ed25519GroupElement h = Ed25519GroupElement.p2(g.getX(), g.getY(), g.getZ().multiply(Ed25519Field.TWO));

			// Assert (can only fail for 5*Z^2=1):
			Assert.assertThat(h.isOnCurve(), IsEqual.equalTo(false));
		}
	}
}

package org.nem.core.model.primitive;

import java.math.BigInteger;

public class AbstractPrimitiveBigIntegerTest extends AbstractPrimitiveTest<
		AbstractPrimitiveBigIntegerTest.FooPrimitive,
		AbstractPrimitiveBigIntegerTest.BarPrimitive,
		BigInteger> {

	@Override
	protected FooPrimitive createFoo(final long value) {
		return new FooPrimitive(this.longToValue(value));
	}

	@Override
	protected BarPrimitive createBar(final long value) {
		return new BarPrimitive(this.longToValue(value));
	}

	@Override
	protected BigInteger longToValue(final long value) {
		return BigInteger.valueOf(value);
	}

	public static class FooPrimitive extends AbstractPrimitive<FooPrimitive, BigInteger> {
		protected FooPrimitive(final BigInteger value) {
			super(value, FooPrimitive.class);
		}
	}

	public static class BarPrimitive extends AbstractPrimitive<BarPrimitive, BigInteger> {
		protected BarPrimitive(final BigInteger value) {
			super(value, BarPrimitive.class);
		}
	}
}
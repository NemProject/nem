package org.nem.core.model.primitive;

public class AbstractPrimitiveLongTest extends AbstractPrimitiveTest<
		AbstractPrimitiveLongTest.FooPrimitive,
		AbstractPrimitiveLongTest.BarPrimitive,
		Long> {

	@Override
	protected FooPrimitive createFoo(final long value) {
		return new FooPrimitive(value);
	}

	@Override
	protected BarPrimitive createBar(final long value) {
		return new BarPrimitive(value);
	}

	@Override
	protected Long longToValue(final long value) {
		return value;
	}

	public static class FooPrimitive extends AbstractPrimitive<FooPrimitive, Long> {
		protected FooPrimitive(final long value) {
			super(value, FooPrimitive.class);
		}
	}

	public static class BarPrimitive extends AbstractPrimitive<BarPrimitive, Long> {
		protected BarPrimitive(final long value) {
			super(value, BarPrimitive.class);
		}
	}
}
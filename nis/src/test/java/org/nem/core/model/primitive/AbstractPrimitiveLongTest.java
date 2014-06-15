package org.nem.core.model.primitive;

public class AbstractPrimitiveLongTest extends AbstractPrimitiveTest<
		AbstractPrimitiveLongTest.FooPrimitive,
		AbstractPrimitiveLongTest.BarPrimitive,
		Long> {

	@Override
	protected FooPrimitive createFoo(long value) {
		return new FooPrimitive(value);
	}

	@Override
	protected BarPrimitive createBar(long value) {
		return new BarPrimitive(value);
	}

	@Override
	protected Long longToValue(long value) {
		return value;
	}

	public static class FooPrimitive extends AbstractPrimitive<FooPrimitive, Long> {
		protected FooPrimitive(long value) {
			super(value, FooPrimitive.class);
		}
	}

	public static class BarPrimitive extends AbstractPrimitive<BarPrimitive, Long> {
		protected BarPrimitive(long value) {
			super(value, BarPrimitive.class);
		}
	}
}
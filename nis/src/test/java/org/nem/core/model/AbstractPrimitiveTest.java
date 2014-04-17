package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

public class AbstractPrimitiveTest {

	//region constructor

	@Test
	public void primitiveCanBeConstructedWithInitialValue() {
		// Arrange:
		final FooPrimitive foo1 = new FooPrimitive(-127);

		// Assert:
		Assert.assertThat(foo1.getRaw(), IsEqual.equalTo(-127L));
	}

	//endregion

	//region compareTo

	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final FooPrimitive foo1 = new FooPrimitive(7);
		final FooPrimitive foo2 = new FooPrimitive(7);

		// Assert:
		Assert.assertThat(foo1.compareTo(foo2), IsEqual.equalTo(0));
		Assert.assertThat(foo2.compareTo(foo1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final FooPrimitive foo1 = new FooPrimitive(7);
		final FooPrimitive foo2 = new FooPrimitive(8);

		// Assert:
		Assert.assertThat(foo1.compareTo(foo2), IsEqual.equalTo(-1));
		Assert.assertThat(foo2.compareTo(foo1), IsEqual.equalTo(1));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final FooPrimitive foo = new FooPrimitive(7);

		// Assert:
		Assert.assertThat(new FooPrimitive(7), IsEqual.equalTo(foo));
		Assert.assertThat(new BarPrimitive(7), IsNot.not((Object) IsEqual.equalTo(foo)));
		Assert.assertThat(new FooPrimitive(6), IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(new FooPrimitive(8), IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)foo)));
	}

	@Test
	public void hashCodesAreOnlyEqualForEquivalentObjects() {
		// Arrange:
		final FooPrimitive foo = new FooPrimitive(7);
		final int hashCode = foo.hashCode();

		// Assert:
		Assert.assertThat(new FooPrimitive(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new BarPrimitive(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new FooPrimitive(6).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new FooPrimitive(8).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsRawFooPrimitive() {
		// Arrange:
		final FooPrimitive foo = new FooPrimitive(22561);

		// Assert:
		Assert.assertThat(foo.toString(), IsEqual.equalTo("22561"));
	}

	//endregion

	//region primitives

	private static class FooPrimitive extends AbstractPrimitive<FooPrimitive>
	{
		protected FooPrimitive(long value) {
			super(value, FooPrimitive.class);
		}

		public long getRaw() { return this.getValue(); }
	}

	private static class BarPrimitive extends AbstractPrimitive<BarPrimitive>
	{
		protected BarPrimitive(long value) {
			super(value, BarPrimitive.class);
		}
	}

	//endregion
}

package org.nem.core.model.primitive;

import org.hamcrest.core.*;
import org.junit.*;

/**
 * Base class for AbstractPrimitive tests.
 *
 * @param <TFoo> The first primitive type.
 * @param <TBar> The second primitive type.
 * @param <TValue> The primitive value type.
 */
public abstract class AbstractPrimitiveTest<
		TFoo extends AbstractPrimitive<TFoo, TValue>,
		TBar extends AbstractPrimitive<TBar, TValue>,
		TValue extends Number & Comparable<TValue>> {

	/**
	 * Creates an instance of the foo primitive type.
	 *
	 * @param value The long value.
	 * @return The foo primitive.
	 */
	protected abstract TFoo createFoo(final long value);

	/**
	 * Creates an instance of the bar primitive type.
	 *
	 * @param value The long value.
	 * @return The bar primitive.
	 */
	protected abstract TBar createBar(final long value);

	/**
	 * Converts a long value into the primitive value type.
	 *
	 * @param value The long value.
	 * @return The primitive value.
	 */
	protected abstract TValue longToValue(long value);

	//region constructor

	@Test
	public void primitiveCanBeConstructedWithInitialValue() {
		// Arrange:
		final TFoo foo = this.createFoo(-127);

		// Assert:
		Assert.assertThat(foo.getValue(), IsEqual.equalTo(this.longToValue(-127L)));
	}

	//endregion

	//region compareTo

	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final TFoo foo1 = this.createFoo(7);
		final TFoo foo2 = this.createFoo(7);

		// Assert:
		Assert.assertThat(foo1.compareTo(foo2), IsEqual.equalTo(0));
		Assert.assertThat(foo2.compareTo(foo1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final TFoo foo1 = this.createFoo(7);
		final TFoo foo2 = this.createFoo(8);

		// Assert:
		Assert.assertThat(foo1.compareTo(foo2), IsEqual.equalTo(-1));
		Assert.assertThat(foo2.compareTo(foo1), IsEqual.equalTo(1));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final TFoo foo = this.createFoo(7);

		// Assert:
		Assert.assertThat(this.createFoo(7), IsEqual.equalTo(foo));
		Assert.assertThat(this.createBar(7), IsNot.not((Object)IsEqual.equalTo(foo)));
		Assert.assertThat(this.createFoo(6), IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(this.createFoo(8), IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)foo)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final TFoo foo = this.createFoo(7);
		final int hashCode = foo.hashCode();

		// Assert:
		Assert.assertThat(this.createFoo(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(this.createBar(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(this.createFoo(6).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(this.createFoo(8).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsRawPrimitiveValue() {
		// Arrange:
		final TFoo foo = this.createFoo(22561);

		// Assert:
		Assert.assertThat(foo.toString(), IsEqual.equalTo("22561"));
	}

	//endregion
}
package org.nem.core.model.primitive;

import java.math.BigInteger;
import org.hamcrest.core.*;
import org.junit.*;

//TODO: refactor tests

public class AbstractPrimitiveTest {

	//region constructor

	@Test
	public void primitiveCanBeConstructedWithInitialValue() {
		// Arrange:
		final FooPrimitive foo1 = new FooPrimitive(-127);
		final BazPrimitive baz1 = new BazPrimitive(BigInteger.valueOf(-127));

		// Assert:
		Assert.assertThat(foo1.getRaw(), IsEqual.equalTo(-127L));
		Assert.assertThat(baz1.getRaw(), IsEqual.equalTo(BigInteger.valueOf(-127L)));
	}

	//endregion

	//region compareTo

	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final FooPrimitive foo1 = new FooPrimitive(7);
		final FooPrimitive foo2 = new FooPrimitive(7);
		final BazPrimitive baz1 = new BazPrimitive(BigInteger.valueOf(-127));
		final BazPrimitive baz2 = new BazPrimitive(BigInteger.valueOf(-127));

		// Assert:
		Assert.assertThat(foo1.compareTo(foo2), IsEqual.equalTo(0));
		Assert.assertThat(foo2.compareTo(foo1), IsEqual.equalTo(0));
		Assert.assertThat(baz1.compareTo(baz2), IsEqual.equalTo(0));
		Assert.assertThat(baz2.compareTo(baz1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final FooPrimitive foo1 = new FooPrimitive(7);
		final FooPrimitive foo2 = new FooPrimitive(8);
		final BazPrimitive baz1 = new BazPrimitive(BigInteger.valueOf(7));
		final BazPrimitive baz2 = new BazPrimitive(BigInteger.valueOf(8));

		// Assert:
		Assert.assertThat(foo1.compareTo(foo2), IsEqual.equalTo(-1));
		Assert.assertThat(foo2.compareTo(foo1), IsEqual.equalTo(1));
		Assert.assertThat(baz1.compareTo(baz2), IsEqual.equalTo(-1));
		Assert.assertThat(baz2.compareTo(baz1), IsEqual.equalTo(1));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final FooPrimitive foo = new FooPrimitive(7);
		final BazPrimitive baz = new BazPrimitive(BigInteger.valueOf(7));

		// Assert:
		Assert.assertThat(new FooPrimitive(7), IsEqual.equalTo(foo));
		Assert.assertThat(new BarPrimitive(7), IsNot.not((Object) IsEqual.equalTo(foo)));
		Assert.assertThat(new FooPrimitive(6), IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(new FooPrimitive(8), IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(foo)));
		Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)foo)));

		Assert.assertThat(new BazPrimitive(BigInteger.valueOf(7)), IsEqual.equalTo(baz));
		Assert.assertThat(new QuxPrimitive(BigInteger.valueOf(7)), IsNot.not((Object) IsEqual.equalTo(baz)));
		Assert.assertThat(new BazPrimitive(BigInteger.valueOf(6)), IsNot.not(IsEqual.equalTo(baz)));
		Assert.assertThat(new BazPrimitive(BigInteger.valueOf(8)), IsNot.not(IsEqual.equalTo(baz)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(baz)));
		Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)baz)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final FooPrimitive foo = new FooPrimitive(7);
		final int hashCode = foo.hashCode();

		// Assert:
		Assert.assertThat(new FooPrimitive(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new BarPrimitive(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new BazPrimitive(BigInteger.valueOf(7)).hashCode(), IsEqual.equalTo(hashCode));
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

	@Test
	public void toStringReturnsRawBazPrimitive() {
		// Arrange:
		final BazPrimitive baz = new BazPrimitive(BigInteger.valueOf(22561));

		// Assert:
		Assert.assertThat(baz.toString(), IsEqual.equalTo("22561"));
	}

	//endregion

	//region primitives

	private static class FooPrimitive extends AbstractPrimitive<FooPrimitive, Long>
	{
		protected FooPrimitive(long value) {
			super(value, FooPrimitive.class);
		}

		public long getRaw() { return this.getValue(); }
	}

	private static class BarPrimitive extends AbstractPrimitive<BarPrimitive, Long>
	{
		protected BarPrimitive(long value) {
			super(value, BarPrimitive.class);
		}
	}

	private static class BazPrimitive extends AbstractPrimitive<BazPrimitive, BigInteger>
	{
		protected BazPrimitive(BigInteger value) {
			super(value, BazPrimitive.class);
		}

		public BigInteger getRaw() { return this.getValue(); }
	}

	private static class QuxPrimitive extends AbstractPrimitive<QuxPrimitive, BigInteger>
	{
		protected QuxPrimitive(BigInteger value) {
			super(value, QuxPrimitive.class);
		}
	}

	//endregion
}

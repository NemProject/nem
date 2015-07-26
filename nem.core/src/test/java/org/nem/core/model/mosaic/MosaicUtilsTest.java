package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.ExceptionAssert;

@RunWith(Enclosed.class)
public class MosaicUtilsTest {

	//region add / tryAdd

	private static abstract class AddBaseTest {

		@Test
		public void canAddUpToMaxQuantityWhenDivisibilityIsZero() {
			// Assert:
			this.assertCanAdd(0, MosaicConstants.MAX_QUANTITY - 100, 10, MosaicConstants.MAX_QUANTITY - 90);
			this.assertCanAdd(0, MosaicConstants.MAX_QUANTITY - 100, 99, MosaicConstants.MAX_QUANTITY - 1);
			this.assertCanAdd(0, MosaicConstants.MAX_QUANTITY - 100, 100, MosaicConstants.MAX_QUANTITY);
		}

		@Test
		public void cannotAddUpToGreaterThanMaxQuantityWhenDivisibilityIsZero() {
			// Assert:
			this.assertCannotAdd(0, MosaicConstants.MAX_QUANTITY - 100, 101);
			this.assertCannotAdd(0, MosaicConstants.MAX_QUANTITY - 100, 999);
		}

		@Test
		public void canAddUpToAdjustedMaxQuantityWhenDivisibilityIsNonZero() {
			// Assert:
			final long maxQuantity = MosaicConstants.MAX_QUANTITY / 1000;
			this.assertCanAdd(3, maxQuantity - 100, 10, maxQuantity - 90);
			this.assertCanAdd(3, maxQuantity - 100, 99, maxQuantity - 1);
			this.assertCanAdd(3, maxQuantity - 100, 100, maxQuantity);
		}

		@Test
		public void cannotAddUpToGreaterThanAdjustedMaxQuantityWhenDivisibilityIsZero() {
			// Assert:
			final long maxQuantity = MosaicConstants.MAX_QUANTITY / 1000;
			this.assertCannotAdd(3, maxQuantity - 100, 101);
			this.assertCannotAdd(3, maxQuantity - 100, 999);
			this.assertCannotAdd(3, MosaicConstants.MAX_QUANTITY - 100, 10);
		}

		protected abstract void assertCannotAdd(final int divisibility, final long q1, final long q2);

		protected abstract void assertCanAdd(final int divisibility, final long q1, final long q2, final long expectedSum);
	}

	public static class AddTest extends AddBaseTest {

		@Override
		protected void assertCannotAdd(final int divisibility, final long q1, final long q2) {
			// Act:
			ExceptionAssert.assertThrows(
					v -> MosaicUtils.add(divisibility, new Quantity(q1), new Quantity(q2)),
					IllegalArgumentException.class);
		}

		@Override
		protected void assertCanAdd(final int divisibility, final long q1, final long q2, final long expectedSum) {
			// Act:
			final Quantity sum = MosaicUtils.add(divisibility, new Quantity(q1), new Quantity(q2));

			// Assert:
			Assert.assertThat(sum, IsEqual.equalTo(new Quantity(expectedSum)));
		}
	}

	public static class TryAddTest extends AddBaseTest {

		@Override
		protected void assertCannotAdd(final int divisibility, final long q1, final long q2) {
			// Act:
			final Quantity sum = MosaicUtils.tryAdd(divisibility, new Quantity(q1), new Quantity(q2));

			// Assert:
			Assert.assertThat(sum, IsNull.nullValue());
		}

		@Override
		protected void assertCanAdd(final int divisibility, final long q1, final long q2, final long expectedSum) {
			// Act:
			final Quantity sum = MosaicUtils.tryAdd(divisibility, new Quantity(q1), new Quantity(q2));

			// Assert:
			Assert.assertThat(sum, IsEqual.equalTo(new Quantity(expectedSum)));
		}
	}

	//endregion
}
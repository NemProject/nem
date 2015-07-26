package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.primitive.*;
import org.nem.core.test.ExceptionAssert;

@RunWith(Enclosed.class)
public class MosaicUtilsTest {

	//region add / tryAdd

	private static abstract class AddBaseTest {

		@Test
		public void canAddUpToMaxSupplyWhenDivisibilityIsZero() {
			// Assert:
			this.assertCanAdd(0, MosaicConstants.MAX_QUANTITY - 100, 10, MosaicConstants.MAX_QUANTITY - 90);
			this.assertCanAdd(0, MosaicConstants.MAX_QUANTITY - 100, 99, MosaicConstants.MAX_QUANTITY - 1);
			this.assertCanAdd(0, MosaicConstants.MAX_QUANTITY - 100, 100, MosaicConstants.MAX_QUANTITY);
		}

		@Test
		public void cannotAddUpToGreaterThanMaxSupplyWhenDivisibilityIsZero() {
			// Assert:
			this.assertCannotAdd(0, MosaicConstants.MAX_QUANTITY - 100, 101);
			this.assertCannotAdd(0, MosaicConstants.MAX_QUANTITY - 100, 999);
		}

		@Test
		public void canAddUpToAdjustedMaxSupplyWhenDivisibilityIsNonZero() {
			// Assert:
			final long maxSupply = MosaicConstants.MAX_QUANTITY / 1000;
			this.assertCanAdd(3, maxSupply - 100, 10, maxSupply - 90);
			this.assertCanAdd(3, maxSupply - 100, 99, maxSupply - 1);
			this.assertCanAdd(3, maxSupply - 100, 100, maxSupply);
		}

		@Test
		public void cannotAddUpToGreaterThanAdjustedMaxSupplyWhenDivisibilityIsZero() {
			// Assert:
			final long maxSupply = MosaicConstants.MAX_QUANTITY / 1000;
			this.assertCannotAdd(3, maxSupply - 100, 101);
			this.assertCannotAdd(3, maxSupply - 100, 999);
			this.assertCannotAdd(3, MosaicConstants.MAX_QUANTITY - 100, 10);
		}

		protected abstract void assertCannotAdd(final int divisibility, final long s1, final long s2);

		protected abstract void assertCanAdd(final int divisibility, final long s1, final long s2, final long expectedSum);
	}

	public static class AddTest extends AddBaseTest {

		@Override
		protected void assertCannotAdd(final int divisibility, final long s1, final long s2) {
			// Act:
			ExceptionAssert.assertThrows(
					v -> MosaicUtils.add(divisibility, new Supply(s1), new Supply(s2)),
					IllegalArgumentException.class);
		}

		@Override
		protected void assertCanAdd(final int divisibility, final long s1, final long s2, final long expectedSum) {
			// Act:
			final Supply sum = MosaicUtils.add(divisibility, new Supply(s1), new Supply(s2));

			// Assert:
			Assert.assertThat(sum, IsEqual.equalTo(new Supply(expectedSum)));
		}
	}

	public static class TryAddTest extends AddBaseTest {

		@Override
		protected void assertCannotAdd(final int divisibility, final long s1, final long s2) {
			// Act:
			final Supply sum = MosaicUtils.tryAdd(divisibility, new Supply(s1), new Supply(s2));

			// Assert:
			Assert.assertThat(sum, IsNull.nullValue());
		}

		@Override
		protected void assertCanAdd(final int divisibility, final long s1, final long s2, final long expectedSum) {
			// Act:
			final Supply sum = MosaicUtils.tryAdd(divisibility, new Supply(s1), new Supply(s2));

			// Assert:
			Assert.assertThat(sum, IsEqual.equalTo(new Supply(expectedSum)));
		}
	}

	//endregion
}
package org.nem.core.math;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;
import java.util.stream.*;

/**
 * Test for SparseBitmap, our wrapper for EWAHCompressedBitmap.
 */
public class SparseBitmapTest {

	//region createEmpty

	@Test
	public void createEmptyCanCreateEmptyBitmap() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createEmpty();

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(false));
	}

	//endregion

	//region createFromUnsortedData

	@Test
	public void createFromUnsortedDataCanCreateBitmapAroundNoBits() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromUnsortedData();

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(false));
	}

	@Test
	public void createFromUnsortedDataCanCreateBitmapAroundBitsInSortedOrder() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromUnsortedData(0, 1, 3);

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
	}

	@Test
	public void createFromUnsortedDataCanCreateBitmapAroundBitsInSortedOrderWithDuplicates() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromUnsortedData(0, 1, 1, 3);

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
	}

	@Test
	public void createFromUnsortedDataCanCreateBitmapAroundBitsInUnsortedOrder() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromUnsortedData(1, 0, 3);

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
	}

	//endregion

	//region createFromSortedData

	@Test
	public void createFromSortedDataCanCreateBitmapAroundNoBits() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromSortedData();

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(false));
	}

	@Test
	public void createFromSortedDataCanCreateBitmapAroundBitsInSortedOrder() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromUnsortedData(0, 1, 3);

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
	}

	@Test
	public void createFromSortedDataCanCreateBitmapAroundBitsInSortedOrderWithDuplicates() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(0, 1, 1, 3);

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
	}

	//endregion

	//region get / set

	@Test
	public void getReturnsFalseOutOfBounds() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromUnsortedData(0, 1, 3);

		// Assert:
		Assert.assertThat(sb.get(-1000), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(-1), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(1000), IsEqual.equalTo(false));
	}

	@Test
	public void bitsCanBeSetInOrder() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromSortedData();
		sb.set(0);
		sb.set(100);
		sb.set(100);
		sb.set(2000000);

		// Assert:
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(100), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2000000), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(false));
	}

	@Test
	public void bitsCannotBeSetOutOfOrder() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromSortedData();
		sb.set(2000000);

		// Assert:
		ExceptionAssert.assertThrows(v -> sb.set(100), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> sb.set(0), IllegalArgumentException.class);
	}

	@Test
	public void bitsCanBeSetInOrderWithoutAscendingCheck() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromSortedData();
		sb.setWithoutAscendingCheck(0);
		sb.setWithoutAscendingCheck(100);
		sb.setWithoutAscendingCheck(100);
		sb.setWithoutAscendingCheck(2000000);

		// Assert:
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(100), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2000000), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(false));
	}

	@Test
	public void bitsCanBeSetOutOfOrderWithoutAscendingCheck() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromSortedData();
		sb.setWithoutAscendingCheck(2000000);
		sb.setWithoutAscendingCheck(100);
		sb.setWithoutAscendingCheck(100);
		sb.setWithoutAscendingCheck(0);

		// Assert: the correct bits are set
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(100), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2000000), IsEqual.equalTo(true));
	}

	//endregion

	//region clear

	@Test
	public void clearZerosOutAllBits() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(4, 5, 6);

		// Act:
		sb.clear();

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(5), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(6), IsEqual.equalTo(false));
	}

	@Test
	public void bitsCanBeSetAfterClearing() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(4, 5, 6);

		// Act:
		sb.clear();
		sb.set(3);

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(1));
		Assert.assertThat(sb.get(4), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(5), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(6), IsEqual.equalTo(false));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
	}

	//endregion

	//region getHighestBit

	@Test
	public void getHighestBitReturnsCorrectResultWhenNoBitsAreSet() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromSortedData();

		// Assert:
		Assert.assertThat(sb.getHighestBit(), IsEqual.equalTo(0));
	}

	@Test
	public void getHighestBitReturnsCorrectResultWhenBitsAreSet() {
		// Act:
		final SparseBitmap sb = SparseBitmap.createFromUnsortedData(0, 1, 7);

		// Assert:
		Assert.assertThat(sb.getHighestBit(), IsEqual.equalTo(7));
	}

	//endregion

	//region batchOr

	@Test
	public void batchOrCanCreateBitmapFromZeroBitmaps() {
		// Act:
		final SparseBitmap sb = SparseBitmap.batchOr();

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(0));
	}

	@Test
	public void batchOrCanCreateBitmapFromOneBitmap() {
		// Act:
		final SparseBitmap sb = SparseBitmap.batchOr(SparseBitmap.createFromSortedData(1, 3));

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(2));
		Assert.assertThat(sb.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
	}

	@Test
	public void batchOrCanCreateBitmapFromMultipleBitmaps() {
		// Act:
		final SparseBitmap sb = SparseBitmap.batchOr(
				SparseBitmap.createFromSortedData(1, 3),
				SparseBitmap.createFromSortedData(5, 6),
				SparseBitmap.createFromSortedData(2, 7));

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(6));
		Assert.assertThat(sb.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(3), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(5), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(6), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb.get(7), IsEqual.equalTo(true));
	}

	//endregion

	//region toList / iterator

	@Test
	public void toListReturnsSetBits() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(100, 200, 300);

		// Assert:
		Assert.assertThat(sb.toList(), IsEqual.equalTo(Arrays.asList(100, 200, 300)));
	}

	@Test
	public void iteratorReturnsSetBits() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(100, 200, 300);

		// Assert:
		Assert.assertThat(
				StreamSupport.stream(sb.spliterator(), false).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(100, 200, 300)));
	}

	//endregion

	//region or / and / andNot

	@Test
	public void orCreatesCorrectBitmap() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(2, 4, 8);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(4, 16, 256);

		// Act:
		final SparseBitmap result = sb1.or(sb2);

		// Assert:

		Assert.assertThat(result.cardinality(), IsEqual.equalTo(5));
		Assert.assertThat(result.get(2), IsEqual.equalTo(true));
		Assert.assertThat(result.get(4), IsEqual.equalTo(true));
		Assert.assertThat(result.get(8), IsEqual.equalTo(true));
		Assert.assertThat(result.get(16), IsEqual.equalTo(true));
		Assert.assertThat(result.get(256), IsEqual.equalTo(true));
	}

	@Test
	public void andCreatesCorrectBitmap() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(2, 4, 8);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(4, 16, 256);

		// Act:
		final SparseBitmap result = sb1.and(sb2);

		// Assert:

		Assert.assertThat(result.cardinality(), IsEqual.equalTo(1));
		Assert.assertThat(result.get(4), IsEqual.equalTo(true));
	}

	@Test
	public void andNotCreatesCorrectBitmap() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(2, 4, 8);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(4, 16, 256);

		// Act:
		final SparseBitmap result = sb1.andNot(sb2);

		// Assert:

		Assert.assertThat(result.cardinality(), IsEqual.equalTo(2));
		Assert.assertThat(result.get(2), IsEqual.equalTo(true));
		Assert.assertThat(result.get(8), IsEqual.equalTo(true));
	}

	//endregion

	//region cardinality / andCardinality

	@Test
	public void cardinalityReturnsTheNumberOfSetBits() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(100, 200, 300, 400, 1337);

		// Assert:
		Assert.assertThat(sb.cardinality(), IsEqual.equalTo(5));
	}

	@Test
	public void andCardinalityReturnsTheNumberOfIntersectingBits() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(2, 4, 8, 16);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(4, 16, 256);

		// Act:
		final int result = sb1.andCardinality(sb2);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(2));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsCorrectRepresentationForEmptyBitmap() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData();

		// Assert:
		Assert.assertThat(sb.toString(), IsEqual.equalTo("{}"));
	}

	@Test
	public void toStringReturnsCorrectRepresentationForNonEmptyBitmap() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(100, 200, 300, 400, 1337);

		// Assert:
		Assert.assertThat(sb.toString(), IsEqual.equalTo("{100,200,300,400,1337}"));
	}

	//endregion

	//region hashCode/equals

	private static final Map<String, SparseBitmap> DESC_TO_SB_MAP = new HashMap<String, SparseBitmap>() {
		{
			this.put("default", SparseBitmap.createFromSortedData(4, 8, 16));
			this.put("diff-short", SparseBitmap.createFromSortedData(4, 8));
			this.put("diff-long", SparseBitmap.createFromSortedData(4, 8, 16, 32));
			this.put("diff-values", SparseBitmap.createFromSortedData(4, 9, 16));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(4, 8, 16);

		// Assert:
		Assert.assertThat(DESC_TO_SB_MAP.get("default"), IsEqual.equalTo(sb));
		Assert.assertThat(DESC_TO_SB_MAP.get("diff-short"), IsNot.not(IsEqual.equalTo(sb)));
		Assert.assertThat(DESC_TO_SB_MAP.get("diff-long"), IsNot.not(IsEqual.equalTo(sb)));
		Assert.assertThat(DESC_TO_SB_MAP.get("diff-values"), IsNot.not(IsEqual.equalTo(sb)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(sb)));
		Assert.assertThat(8, IsNot.not(IsEqual.equalTo((Object)sb)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final SparseBitmap sb = SparseBitmap.createFromSortedData(4, 8, 16);
		final int hashCode = sb.hashCode();

		// Assert:
		Assert.assertThat(DESC_TO_SB_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(DESC_TO_SB_MAP.get("diff-short").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_SB_MAP.get("diff-long").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(DESC_TO_SB_MAP.get("diff-values").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion
}

package org.nem.core.math;

import org.apache.commons.collections15.IteratorUtils;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.*;

/**
 * Test for SparseBitmap, our wrapper for EWAHCompressedBitmap.
 */
// TODO-CR [08062014][J-M]: just a feeling that the tests are doing just a little too much (e.g. there should be one test for each type of construction vs a single constructor test that tests all)
	// the reason is that single large tests can hide failures (if the first thing out of 10 fails, there could be 1-10 "real" failures)
public class SparseBitmapTest {

	// Section creation factor method
	@Test
	public void createFromUnsortedDataSortsCorrectly() {
		// Arrange and Act:
		final SparseBitmap sb1 = SparseBitmap.createFromUnsortedData();
		final SparseBitmap sb2 = SparseBitmap.createFromUnsortedData(0,1,2);
		final SparseBitmap sb3 = SparseBitmap.createFromUnsortedData(2, 1, 0); //test auto-sort
		final SparseBitmap sb4 = SparseBitmap.createFromUnsortedData(new int[]{0,1,2});

		// Assert:
		Assert.assertThat(sb1.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb1.get(0), IsEqual.equalTo(false));
		
		Assert.assertThat(sb2.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb2.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(3), IsEqual.equalTo(false));
		
		Assert.assertThat(sb3.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb3.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb3.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb3.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb3.get(3), IsEqual.equalTo(false));
		
		Assert.assertThat(sb4.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb4.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb4.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb4.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb4.get(3), IsEqual.equalTo(false));
	}
	
	@Test
	public void createFromSortedDataCreatesCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(2,1,0);

		// Assert:
		Assert.assertThat(sb1.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb1.get(0), IsEqual.equalTo(false));
		
		Assert.assertThat(sb2.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb2.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(3), IsEqual.equalTo(false));
		
		Assert.assertThat(sb3.cardinality(), IsEqual.equalTo(1));
		Assert.assertThat(sb3.get(0), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(1), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb3.get(3), IsEqual.equalTo(false));
	}
	// End Section creation factory methods
	
	// Section constructor Get/Set/setWithoutAscendingCheck/Clear
	@Test
	public void getReturnsCorrectly() {
		
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(2,1,0);

		// Act and Assert:
		for (int i=0; i < 100; ++i) {
			Assert.assertThat(sb1.get(i), IsEqual.equalTo(false));
			Assert.assertThat(sb1.get(-i), IsEqual.equalTo(false));
		}
		
		Assert.assertThat(sb2.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(3), IsEqual.equalTo(false));
		Assert.assertThat(sb2.get(-3), IsEqual.equalTo(false));

		Assert.assertThat(sb3.get(0), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(1), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb3.get(3), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(-3), IsEqual.equalTo(false));
	}
	
	@Test
	public void setWorksCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData();
		
		// Act:
		sb1.set(0);
		sb1.set(1);
		sb1.set(2);
		
		sb2.set(0);
		sb2.set(100);
		sb2.set(2000000);
		
		// Test setting out of order
		sb3.set(2);
		
		ExceptionAssert.assertThrows(v -> {
			// Act:
			sb3.set(1);
		}, IllegalArgumentException.class);
		
		ExceptionAssert.assertThrows(v -> {
			// Act:
			sb1.set(0);
		}, IllegalArgumentException.class);

		// Assert:
		Assert.assertThat(sb1.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb1.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb1.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb1.get(3), IsEqual.equalTo(false));
		
		Assert.assertThat(sb2.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(100), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(2000000), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(3), IsEqual.equalTo(false));
		
		// Test case that user messes up and does it out of order.
		Assert.assertThat(sb3.get(0), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(1), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb3.get(3), IsEqual.equalTo(false));
	}
	
	@Test
	public void setWithoutAscendingCheckWorksCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData();
		
		// Act:
		sb1.setWithoutAscendingCheck(0);
		sb1.setWithoutAscendingCheck(1);
		sb1.setWithoutAscendingCheck(2);
		
		sb2.setWithoutAscendingCheck(0);
		sb2.setWithoutAscendingCheck(100);
		sb2.setWithoutAscendingCheck(2000000);
		
		// Test setting out of order
		sb3.setWithoutAscendingCheck(2);
		sb3.setWithoutAscendingCheck(1);
		sb3.setWithoutAscendingCheck(0);

		// Assert:
		Assert.assertThat(sb1.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb1.get(1), IsEqual.equalTo(true));
		Assert.assertThat(sb1.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb1.get(3), IsEqual.equalTo(false));
		
		Assert.assertThat(sb2.get(0), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(100), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(2000000), IsEqual.equalTo(true));
		Assert.assertThat(sb2.get(3), IsEqual.equalTo(false));
		
		// Test case that user messes up and does it out of order.
		Assert.assertThat(sb3.get(0), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(1), IsEqual.equalTo(false));
		Assert.assertThat(sb3.get(2), IsEqual.equalTo(true));
		Assert.assertThat(sb3.get(3), IsEqual.equalTo(false));
	}
	
	@Test
	public void clearWorksCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromUnsortedData();
		final SparseBitmap sb2 = SparseBitmap.createFromUnsortedData(0,1,2);
		final SparseBitmap sb3 = SparseBitmap.createFromUnsortedData(2, 1, 0);
		final SparseBitmap sb4 = SparseBitmap.createFromUnsortedData(new int[]{0,1,2});
		
		// Act:
		sb1.clear();
		sb2.clear();
		sb3.clear();
		sb4.clear();

		// Assert:
		Assert.assertThat(sb1.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb2.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb3.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb4.cardinality(), IsEqual.equalTo(0));
	}
	// End Section constructor Get/Set/Clear
	
	// Section batchOr
	@Test
	public void batchOrWorksCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		
		// Act:
		final SparseBitmap bitmap = SparseBitmap.batchOr(sb1,sb2,sb3,sb4);

		// Assert:
		Assert.assertThat(bitmap.cardinality(), IsEqual.equalTo(9));
		Assert.assertThat(bitmap.get(0), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(1), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(2), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(3), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(4), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(5), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(100), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(200), IsEqual.equalTo(true));
		Assert.assertThat(bitmap.get(300), IsEqual.equalTo(true));
		
		Assert.assertThat(bitmap.get(1337), IsEqual.equalTo(false));
		Assert.assertThat(bitmap.get(1338), IsEqual.equalTo(false));
	}
	
	@Test
	public void batchOrHandlesInvalidInputCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData();
		
		// Act:
		final SparseBitmap bitmap1 = SparseBitmap.batchOr(sb1,sb2,sb3,sb4);
		final SparseBitmap bitmap2 = SparseBitmap.batchOr();
		
		// Assert:
		Assert.assertThat(bitmap1.cardinality(), IsEqual.equalTo(0));

		Assert.assertThat(bitmap1.get(0), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(3), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(4), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(5), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(100), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(1337), IsEqual.equalTo(false));
		
		Assert.assertThat(bitmap2.cardinality(), IsEqual.equalTo(0));
		
		Assert.assertThat(bitmap2.get(0), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(3), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(4), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(5), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(100), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(1337), IsEqual.equalTo(false));
	}
	// End Section static or
	
	// Section or/andNot
	@Test
	public void orWorksCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		
		// Act:
		final SparseBitmap bitmap1 = sb1.or(sb2);
		final SparseBitmap bitmap2 = sb2.or(sb3);
		final SparseBitmap bitmap3 = sb3.or(sb4);

		// Assert:
		// bitmap1
		Assert.assertThat(bitmap1.cardinality(), IsEqual.equalTo(4));
		Assert.assertThat(bitmap1.get(0), IsEqual.equalTo(true));
		Assert.assertThat(bitmap1.get(1), IsEqual.equalTo(true));
		Assert.assertThat(bitmap1.get(2), IsEqual.equalTo(true));
		Assert.assertThat(bitmap1.get(3), IsEqual.equalTo(true));
		
		Assert.assertThat(bitmap1.get(4), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(5), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(100), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(200), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(300), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(1337), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(1338), IsEqual.equalTo(false));
		
		// bitmap2
		Assert.assertThat(bitmap2.cardinality(), IsEqual.equalTo(5));
		Assert.assertThat(bitmap2.get(0), IsEqual.equalTo(false));
		
		Assert.assertThat(bitmap2.get(1), IsEqual.equalTo(true));
		Assert.assertThat(bitmap2.get(2), IsEqual.equalTo(true));
		Assert.assertThat(bitmap2.get(3), IsEqual.equalTo(true));
		Assert.assertThat(bitmap2.get(4), IsEqual.equalTo(true));
		Assert.assertThat(bitmap2.get(5), IsEqual.equalTo(true));
		
		Assert.assertThat(bitmap2.get(100), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(200), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(300), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(1337), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(1338), IsEqual.equalTo(false));
	
		// bitmap3
		Assert.assertThat(bitmap3.cardinality(), IsEqual.equalTo(6));
		Assert.assertThat(bitmap3.get(0), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(1), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(2), IsEqual.equalTo(false));
		
		Assert.assertThat(bitmap3.get(3), IsEqual.equalTo(true));
		Assert.assertThat(bitmap3.get(4), IsEqual.equalTo(true));
		Assert.assertThat(bitmap3.get(5), IsEqual.equalTo(true));
		Assert.assertThat(bitmap3.get(100), IsEqual.equalTo(true));
		Assert.assertThat(bitmap3.get(200), IsEqual.equalTo(true));
		Assert.assertThat(bitmap3.get(300), IsEqual.equalTo(true));
		
		Assert.assertThat(bitmap3.get(1337), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(1338), IsEqual.equalTo(false));
	}
	
	@Test
	public void andNotWorksCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		
		// Act:
		final SparseBitmap bitmap1 = sb1.andNot(sb2);
		final SparseBitmap bitmap2 = sb2.andNot(sb3);
		final SparseBitmap bitmap3 = sb3.andNot(sb4);

		// Assert:
		// bitmap1
		Assert.assertThat(bitmap1.cardinality(), IsEqual.equalTo(1));
		Assert.assertThat(bitmap1.get(0), IsEqual.equalTo(true));
		
		Assert.assertThat(bitmap1.get(1), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(2), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(3), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(4), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(5), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(100), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(200), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(300), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(1337), IsEqual.equalTo(false));
		Assert.assertThat(bitmap1.get(1338), IsEqual.equalTo(false));
		
		// bitmap2
		Assert.assertThat(bitmap2.cardinality(), IsEqual.equalTo(2));
		Assert.assertThat(bitmap2.get(0), IsEqual.equalTo(false));
		
		Assert.assertThat(bitmap2.get(1), IsEqual.equalTo(true));
		Assert.assertThat(bitmap2.get(2), IsEqual.equalTo(true));
		
		Assert.assertThat(bitmap2.get(3), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(4), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(5), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(100), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(200), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(300), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(1337), IsEqual.equalTo(false));
		Assert.assertThat(bitmap2.get(1338), IsEqual.equalTo(false));
	
		// bitmap3
		Assert.assertThat(bitmap3.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(bitmap3.get(0), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(1), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(2), IsEqual.equalTo(false));
		
		Assert.assertThat(bitmap3.get(3), IsEqual.equalTo(true));
		Assert.assertThat(bitmap3.get(4), IsEqual.equalTo(true));
		Assert.assertThat(bitmap3.get(5), IsEqual.equalTo(true));
		
		Assert.assertThat(bitmap3.get(100), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(200), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(300), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(1337), IsEqual.equalTo(false));
		Assert.assertThat(bitmap3.get(1338), IsEqual.equalTo(false));
	}
	// End Section or/andNot
	
	// Section cardinality/andCardinality
	@Test
	public void cardinalityReturnsCorrectly() {
		// Arrange and Act:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		final SparseBitmap sb5 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb6 = SparseBitmap.createFromSortedData(100,200,300,400,1337);
		

		// Assert:
		Assert.assertThat(sb1.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb2.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb3.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb4.cardinality(), IsEqual.equalTo(3));
		Assert.assertThat(sb5.cardinality(), IsEqual.equalTo(0));
		Assert.assertThat(sb6.cardinality(), IsEqual.equalTo(5));
	}
	
	@Test
	public void andCardinalityReturnsCorrectly() {
		// Arrange and Act:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		final SparseBitmap sb5 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb6 = SparseBitmap.createFromSortedData(100,200,300,400,1337);
				
		// Assert:
		Assert.assertThat(sb1.andCardinality(sb1), IsEqual.equalTo(3));
		Assert.assertThat(sb1.andCardinality(sb2), IsEqual.equalTo(2));
		Assert.assertThat(sb1.andCardinality(sb3), IsEqual.equalTo(0));
		Assert.assertThat(sb1.andCardinality(sb4), IsEqual.equalTo(0));
		Assert.assertThat(sb1.andCardinality(sb5), IsEqual.equalTo(0));
		Assert.assertThat(sb1.andCardinality(sb6), IsEqual.equalTo(0));
		
		Assert.assertThat(sb4.andCardinality(sb1), IsEqual.equalTo(0));
		Assert.assertThat(sb4.andCardinality(sb2), IsEqual.equalTo(0));
		Assert.assertThat(sb4.andCardinality(sb3), IsEqual.equalTo(0));
		Assert.assertThat(sb4.andCardinality(sb4), IsEqual.equalTo(3));
		Assert.assertThat(sb4.andCardinality(sb5), IsEqual.equalTo(0));
		Assert.assertThat(sb4.andCardinality(sb6), IsEqual.equalTo(3));
	}
	// End Section cardinality/andCardinality
	
	// Section toList
	@Test
	public void toListReturnsCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		final SparseBitmap sb5 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb6 = SparseBitmap.createFromSortedData(100,200,300,400,1337);
		
		// Act:
		List<Integer> v1 = sb1.toList();
		List<Integer> v2 = sb2.toList();
		List<Integer> v3 = sb3.toList();
		List<Integer> v4 = sb4.toList();
		List<Integer> v5 = sb5.toList();
		List<Integer> v6 = sb6.toList();
		
		// Assert:
		Assert.assertThat(v1, IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{0,1,2}))));
		Assert.assertThat(v2, IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{1,2,3}))));
		Assert.assertThat(v3, IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{3,4,5}))));
		Assert.assertThat(v4, IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{100,200,300}))));
		Assert.assertThat(v5, IsEqual.equalTo(new ArrayList<>()));
		Assert.assertThat(v6, IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{100,200,300,400,1337}))));
	}
	// End Section toList
	
	// Section iterator
	@Test
	public void iteratorReturnsCorrectly() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		final SparseBitmap sb5 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb6 = SparseBitmap.createFromSortedData(100,200,300,400,1337);
		
		// Act:
		Iterator<Integer> v1 = sb1.iterator();
		Iterator<Integer> v2 = sb2.iterator();
		Iterator<Integer> v3 = sb3.iterator();
		Iterator<Integer> v4 = sb4.iterator();
		Iterator<Integer> v5 = sb5.iterator();
		Iterator<Integer> v6 = sb6.iterator();
		
		// Assert:
		Assert.assertThat(IteratorUtils.toList(v1), IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{0,1,2}))));
		Assert.assertThat(IteratorUtils.toList(v2), IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{1,2,3}))));
		Assert.assertThat(IteratorUtils.toList(v3), IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{3,4,5}))));
		Assert.assertThat(IteratorUtils.toList(v4), IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{100,200,300}))));
		Assert.assertThat(IteratorUtils.toList(v5), IsEqual.equalTo(new ArrayList<>()));
		Assert.assertThat(IteratorUtils.toList(v6), IsEqual.equalTo(new ArrayList<>(Arrays.asList(new Integer[]{100,200,300,400,1337}))));
	}
	// End Section iterator
	
	// Section toString/equals
	@Test
	public void toStringReturnsCorrectRepresentation() {
		// Arrange:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(0,1,2);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData(100,200,300);
		final SparseBitmap sb5 = SparseBitmap.createFromSortedData();
		final SparseBitmap sb6 = SparseBitmap.createFromSortedData(100,200,300,400,1337);
		
		// Act:
		String v1 = sb1.toString();
		String v2 = sb2.toString();
		String v3 = sb3.toString();
		String v4 = sb4.toString();
		String v5 = sb5.toString();
		String v6 = sb6.toString();
		
		// Assert:
		Assert.assertThat(v1, IsEqual.equalTo("{0,1,2}"));
		Assert.assertThat(v2, IsEqual.equalTo("{1,2,3}"));
		Assert.assertThat(v3, IsEqual.equalTo("{3,4,5}"));
		Assert.assertThat(v4, IsEqual.equalTo("{100,200,300}"));
		Assert.assertThat(v5, IsEqual.equalTo("{}"));
		Assert.assertThat(v6, IsEqual.equalTo("{100,200,300,400,1337}"));
	}

	// End Section toString

	// region hashCode/equals

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange and Act:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		final SparseBitmap sb4 = SparseBitmap.createFromSortedData();

		// Assert:
		Assert.assertThat(sb1.hashCode(), IsEqual.equalTo(sb2.hashCode()));
		Assert.assertThat(sb1.hashCode(), IsNot.not(IsEqual.equalTo(sb3.hashCode())));
		Assert.assertThat(sb1.hashCode(), IsNot.not(IsEqual.equalTo(sb4.hashCode())));
		Assert.assertThat(sb3.hashCode(), IsNot.not(IsEqual.equalTo(sb4.hashCode())));
	}

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange and Act:
		final SparseBitmap sb1 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb2 = SparseBitmap.createFromSortedData(1,2,3);
		final SparseBitmap sb3 = SparseBitmap.createFromSortedData(3,4,5);
		
		// Assert:
		Assert.assertThat(sb1.equals(sb2), IsEqual.equalTo(true));
		Assert.assertThat(sb2.equals(sb1), IsEqual.equalTo(true));
		Assert.assertThat(sb1.equals(sb3), IsEqual.equalTo(false));
		Assert.assertThat(sb2.equals(sb3), IsEqual.equalTo(false));
		Assert.assertThat(sb2.equals(null), IsEqual.equalTo(false));
		Assert.assertThat(sb2.equals(new ArrayList()), IsEqual.equalTo(false));
	}

	// endregion
}

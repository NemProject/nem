package org.nem.core.math;

import com.googlecode.javaewah.EWAHCompressedBitmap;

import java.util.*;

/**
 * This is a wrapper for the EWAHCompressedBitmap.
 */
public class SparseBitmap implements java.lang.Iterable<Integer> {
	private final EWAHCompressedBitmap bitmap;

	// Private constructor
	private SparseBitmap(final EWAHCompressedBitmap bitmap) {
		this.bitmap = bitmap;
	}

	//region factories

	/**
	 * Creates a new <code>SparseBitmap</code> that is empty.
	 *
	 * @return A new <code>SparseBitmap</code> that is empty (no bits set).
	 */
	public static SparseBitmap createEmpty() {
		return new SparseBitmap(EWAHCompressedBitmap.bitmapOf());
	}

	/**
	 * Creates a new <code>SparseBitmap</code> from data that are already sorted
	 * in strictly ascending order (duplicate values are okay).
	 *
	 * @param bitsToSet The bits to set.
	 * @return A new <code>SparseBitmap</code> with the given bits set.
	 */
	public static SparseBitmap createFromSortedData(final int... bitsToSet) {
		return new SparseBitmap(EWAHCompressedBitmap.bitmapOf(bitsToSet));
	}

	/**
	 * Creates a new <code>SparseBitmap</code> from unsorted data.
	 *
	 * @param bitsToSet The bits to set.
	 * @return A new <code>SparseBitmap</code> with the given bits set.
	 */
	public static SparseBitmap createFromUnsortedData(final int... bitsToSet) {
		Arrays.sort(bitsToSet);
		return new SparseBitmap(EWAHCompressedBitmap.bitmapOf(bitsToSet));
	}

	//endregion

	//region get / set

	/**
	 * Gets the value of the bit at the given index.
	 *
	 * @param bitToGet The index of the bit to get.
	 * @return true if the bit is set, false if the bit is not set at the given <code>bitToGet</code> index.
	 */
	public boolean get(final int bitToGet) {
		return this.bitmap.get(bitToGet);
	}

	/**
	 * For speed, this method sets bits at the given index, without checking that the bits
	 * are set in strictly ascending order. If the bits are not in ascending order, performance could be adversely affected.
	 *
	 * @param bitToSet index of the bit to set
	 */
	public void setWithoutAscendingCheck(final int bitToSet) {
		this.bitmap.set(bitToSet);
	}

	/**
	 * Set the bit at the given index. Throws an exception if bits are not set in strictly ascending order.
	 * For performance reasons, bits must be set in ascending order.
	 *
	 * @param bitToSet The index of the bit to set.
	 */
	public void set(final int bitToSet) {
		// Check that we are setting bits in ascending order (equality with the last value is OK).
		if (this.bitmap.cardinality() > 0 && bitToSet < this.bitmap.toArray()[this.bitmap.cardinality() - 1]) {
			throw new IllegalArgumentException("Must set bits in strictly ascending order.");
		}

		this.bitmap.set(bitToSet);
	}

	//endregion

	//region clear

	/**
	 * Clears all the bits in this sparse bitmap.
	 */
	public void clear() {
		this.bitmap.clear();
	}

	//endregion

	//region logical operations

	/**
	 * Creates a new SparseBitmap that is the logical <code>or</code> of all the given bitmaps.
	 *
	 * @param bitmaps Bitmaps to compute the logical <code>or</code> for
	 * @return SparseBitmap that has the values set according to the <code>or</code> of the given bitmaps.
	 */
	public static SparseBitmap batchOr(final SparseBitmap... bitmaps) {
		if (bitmaps.length < 1) {
			return SparseBitmap.createFromUnsortedData();
		}

		if (bitmaps.length < 2) {
			return bitmaps[0];
		}

		EWAHCompressedBitmap firstMap = bitmaps[0].bitmap;

		for (int index = 1; index < bitmaps.length; ++index) {
			firstMap = firstMap.or(bitmaps[index].bitmap);
		}

		return new SparseBitmap(firstMap);
	}

	/**
	 * Computes the logical <code>or</code> of the context bitmap
	 * (<code>this</code>) and the given bitmap.
	 *
	 * @param rhs Bitmap to compute the logical <code>or</code> with.
	 * @return Logical <code>or</code> of <code>this</code> bitmap
	 * (context object) and the given bitmap.
	 */
	public SparseBitmap or(final SparseBitmap rhs) {
		return new SparseBitmap(this.bitmap.or(rhs.bitmap));
	}

	/**
	 * Computes the logical <code>and</code> of the context bitmap
	 * (<code>this</code>) and the given bitmap.
	 *
	 * @param rhs Bitmap to compute the logical <code>and</code> with.
	 * @return Logical <code>and</code> of <code>this</code> bitmap
	 * (context object) and the given bitmap.
	 */
	public SparseBitmap and(final SparseBitmap rhs) {
		return new SparseBitmap(this.bitmap.and(rhs.bitmap));
	}

	/**
	 * Computes the logical <code>and not</code> of the context bitmap
	 * (<code>this</code>) and the given bitmap.
	 *
	 * @param rhs Bitmap to compute the logical <code>and not</code> with.
	 * @return Logical <code>and not</code> of <code>this</code> bitmap
	 * (context object) and the given bitmap.
	 */
	public SparseBitmap andNot(final SparseBitmap rhs) {
		return new SparseBitmap(this.bitmap.andNot(rhs.bitmap));
	}

	//endregion

	//region highest bit / cardinality

	/**
	 * Gets the highest bit that is set in the bitmap.
	 *
	 * @return The highest bit.
	 */
	public int getHighestBit() {
		return this.bitmap.cardinality() > 0 ? this.bitmap.toArray()[this.bitmap.cardinality() - 1] : 0;
	}

	/**
	 * Size of the intersection of <code>this</code> bitmap and
	 * the given bitmap.
	 *
	 * @param rhs given sparse bitmap to compute the size of the intersection of
	 * @return size of the intersection of <code>this</code> bitmap and the given bitmap.
	 */
	public int andCardinality(final SparseBitmap rhs) {
		return this.bitmap.andCardinality(rhs.bitmap);
	}

	/**
	 * The number of bits that are set.
	 *
	 * @return The number of bits that are set.
	 */
	public int cardinality() {
		return this.bitmap.cardinality();
	}

	//endregion

	//region list / iterator

	/**
	 * Creates a binary list representation of this sparse bitmap.
	 *
	 * @return List representation of this sparse bitmap.
	 */
	public List<Integer> toList() {
		return this.bitmap.toList();
	}

	@Override
	public Iterator<Integer> iterator() {
		return this.bitmap.iterator();
	}

	//endregion

	//region hashCode / equals

	@Override
	public int hashCode() {
		return this.bitmap.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SparseBitmap)) {
			return false;
		}

		final SparseBitmap rhs = (SparseBitmap)obj;
		return this.bitmap.equals(rhs.bitmap);
	}

	@Override
	public String toString() {
		return this.bitmap.toString();
	}

	//endregion
}

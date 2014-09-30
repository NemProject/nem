package org.nem.core.math;

import com.googlecode.javaewah.EWAHCompressedBitmap;

import java.util.*;

// TODO-CR [08062014][J-M]: ask BR to autoformat :)
// TODO-CR [20140813][M-BR]: going to switch to intelliJ later

/**
 * This is a wrapper for the EWAHCompressedBitmap. The reason we are wrapping it is
 * because EWAHCompressedBitmap requires that bits be set in strictly increasing order
 * and silent, hard to find bugs can be created if this constraint is not followed.
 */
public class SparseBitmap implements java.lang.Iterable<Integer> {
	private final EWAHCompressedBitmap bitmap;

	// Private constructor
	private SparseBitmap(final EWAHCompressedBitmap bitmap) {
		this.bitmap = bitmap;
	}

	/**
	 * Create a new <code>SparseBitmap</code> from data that are already sorted
	 * in strictly ascending order (duplicate values are okay).
	 *
	 * @param bitsToSet - indices of bits to
	 * @return new <code>SparseBitmap</code> with the given bits set
	 */
	public static SparseBitmap createFromSortedData(int... bitsToSet) {
		return new SparseBitmap(EWAHCompressedBitmap.bitmapOf(bitsToSet));
	}

	/**
	 * Create a new <code>SparseBitmap</code> from data that are not or
	 * may not be in strictly ascending order. This method will sort the data
	 * for you.
	 *
	 * @param bitsToSet The bits to set.
	 * @return new <code>SparseBitmap</code> with the given bits set
	 */
	public static SparseBitmap createFromUnsortedData(int... bitsToSet) {
		Arrays.sort(bitsToSet);
		return new SparseBitmap(EWAHCompressedBitmap.bitmapOf(bitsToSet));
	}

	/**
	 * Gets the value of the bit at the given index.
	 *
	 * @param bitToGet - index of the bit to get the value of
	 * @return - true if the bit is set, false is the bit is not set at the given <code>bitToGet</code> index
	 */
	public boolean get(int bitToGet) {
		return this.bitmap.get(bitToGet);
	}

	/**
	 * For speed, this method sets bits at the given index, without checking that the bits
	 * are set in strictly ascending order. If used incorrectly, this will fail silently, producing
	 * subtle and destructive errors, so caution is advised.
	 *
	 * @param bitToSet index of the bit to set
	 */
	public void setWithoutAscendingCheck(int bitToSet) {
		this.bitmap.set(bitToSet);
	}

	/**
	 * Set the bit at the given index. Throws an exception if bits are not set in strictly ascending order.
	 *
	 * @param bitToSet index of the bit to set
	 */
	public void set(int bitToSet) {
		// Check that we are setting bits in ascending order (equality with the last value is OK).
		if (this.bitmap.cardinality() > 0
				&& bitToSet < this.bitmap.toArray()[this.bitmap.cardinality() - 1]) {
			throw new IllegalArgumentException("Must set bits in strictly ascending order.");
		}
		this.bitmap.set(bitToSet);
	}

	/**
	 * Gets the highest bit which is set in the bitmap.
	 *
	 * @return The highest bit:
	 */
	public int getHighestBit() {
		return this.bitmap.cardinality() > 0 ? this.bitmap.toArray()[this.bitmap.cardinality() - 1] : 0;
	}

	/**
	 * Clears all the bits in this sparse bitmap.
	 */
	public void clear() {
		this.bitmap.clear();
	}

	/**
	 * Creates a new SparseBitmap that is the logical <code>or</code> of all the given bitmaps.
	 *
	 * @param bitmaps Bitmaps to compute the logical <code>or</code> for
	 * @return SparseBitmap that has the values set according to the <code>or</code> of the given bitmaps.
	 */
	public static SparseBitmap batchOr(SparseBitmap... bitmaps) {
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
	 * Creates a binary list representation of this bitset
	 *
	 * @return List representation of this sparse bitmap.
	 */
	public List<Integer> toList() {
		return this.bitmap.toList();
	}

	/**
	 * Computes the logical <code>or</code> of the context bitmap
	 * (<code>this</code>) and the given bitmap.
	 *
	 * @param rhs - bitmap to compute the logical <code>or</code> with.
	 * @return logical <code>or</code> of <code>this</code> bitmap
	 * (context object) and the given bitmap.
	 */
	public SparseBitmap or(SparseBitmap rhs) {
		return new SparseBitmap(this.bitmap.or(rhs.bitmap));
	}

	/**
	 * Computes the logical <code>and not</code> of the context bitmap
	 * (<code>this</code>) and the given bitmap.
	 *
	 * @param rhs - bitmap to compute the logical <code>and not</code> with.
	 * @return logical <code>and not</code> of <code>this</code> bitmap
	 * (context object) and the given bitmap.
	 */
	public SparseBitmap andNot(SparseBitmap rhs) {
		return new SparseBitmap(this.bitmap.andNot(rhs.bitmap));
	}

	/**
	 * Size of the intersection of <code>this</code> bitmap and
	 * the given bitmap.
	 *
	 * @param rhs given sparse bitmap to compute the size of the intersection of
	 * @return size of the intersection of <code>this</code> bitmap and the given bitmap.
	 */
	public int andCardinality(SparseBitmap rhs) {
		return this.bitmap.andCardinality(rhs.bitmap);
	}

	/**
	 * Size.
	 *
	 * @return Size of this sparse bitmap.
	 */
	public int cardinality() {
		return this.bitmap.cardinality();
	}

	@Override
	public Iterator<Integer> iterator() {
		return this.bitmap.iterator();
	}

	@Override
	public String toString() {
		return this.bitmap.toString();
	}

	@Override
	public int hashCode() {
		return this.bitmap.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SparseBitmap)) {
			return false;
		}
		SparseBitmap rhs = (SparseBitmap)obj;
		return this.bitmap.equals(rhs.bitmap);
	}
}

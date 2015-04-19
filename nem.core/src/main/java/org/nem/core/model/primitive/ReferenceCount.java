package org.nem.core.model.primitive;

/**
 * Represents a referencer counter.
 * <br>
 * This class is immutable.
 */
public class ReferenceCount extends AbstractPrimitive<ReferenceCount, Long> {

	/**
	 * Value representing initial referenceCount.
	 */
	public static final ReferenceCount ZERO = new ReferenceCount(0);

	/**
	 * Creates a new reference counter.
	 *
	 * @param refCount The original reference count.
	 */
	public ReferenceCount(final long refCount) {
		super(refCount, ReferenceCount.class);

		if (this.getRaw() < 0) {
			throw new IllegalArgumentException("reference counter can't be negative");
		}
	}

	/**
	 * Returns the underlying reference counter.
	 *
	 * @return The underlying reference counter.
	 */
	public long getRaw() {
		return this.getValue();
	}

	/**
	 * Increments the reference counter.
	 *
	 * @return The incremented reference counter.
	 */
	public ReferenceCount increment() {
		return new ReferenceCount(this.getRaw() + 1);
	}

	/**
	 * Decrements the reference counter.
	 *
	 * @return The decremented reference counter.
	 */
	public ReferenceCount decrement() {
		return new ReferenceCount(this.getRaw() - 1);
	}
}

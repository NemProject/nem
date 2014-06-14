package org.nem.core.model.primitive;

/**
 * Represents a referencer counter.
 */
public class ReferenceCounter extends AbstractPrimitive<ReferenceCounter> {

	/**
	 * Value representing initial referenceCount.
	 */
	public static final ReferenceCounter ZERO = new ReferenceCounter(0);

	/**
	 * Creates a new reference counter.
	 *
	 * @param refCount The original reference count.
	 */
	public ReferenceCounter(final long refCount) {
		super(refCount, ReferenceCounter.class);
		
		if (this.getRaw() < 0)
			throw new IllegalArgumentException("reference counter can't be negative");
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
	 * Increments the reference counter
	 * 
	 * @return The incremented reference counter
	 */
	public ReferenceCounter increment() {
		return new ReferenceCounter(this.getRaw() + 1);
	}
	
	/**
	 * Decrements the reference counter
	 * 
	 * @return The decremented reference counter
	 */
	public ReferenceCounter decrement() {
		return new ReferenceCounter(this.getRaw() - 1);
	}
}
